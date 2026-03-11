package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.comment.CommentResponse;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.TaskComment;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskCommentService {

	private final ApplicationEventPublisher eventPublisher;

	private final TaskCommentRepository taskCommentRepository;
	private final TaskRepository taskRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	/**
	 * 댓글/대댓글 생성
	 * 권한 = validatePermission() 참고
	 */
	public void createComment(UUID authorPublicId, UUID projectPublicId, UUID taskPublicId,
							  String content, Long parentCommentId) {
		User author = validatePermission(authorPublicId, projectPublicId);

		Task task = taskRepository.findTaskWithMembersByPublicIds(taskPublicId, projectPublicId)
								  .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		TaskComment comment;

		if (parentCommentId != null) {
			TaskComment parentComment = taskCommentRepository.findByIdWithAuthor(parentCommentId)
															 .orElseThrow(() -> new CustomException(COMMENT_NOT_FOUND));

			// 대댓글의 대댓글 방지 (2depth 제한)
			if (parentComment.getParentComment() != null) {
				throw new CustomException(COMMENT_REPLY_DEPTH_EXCEEDED);
			}

			comment = TaskComment.createReply(task, author, content, parentComment);
		} else {
			comment = TaskComment.createComment(task, author, content);
		}

		taskCommentRepository.save(comment);
		log.info("댓글 생성 - taskId={}, authorId={}, 대댓글 여부={}", taskPublicId, authorPublicId, parentCommentId != null);

		sendNotifications(task, projectPublicId, author, comment);
	}

	/**
	 * 댓글 목록 조회
	 * 권한 = validatePermission() 참고
	 */
	@Transactional(readOnly = true)
	public List<CommentResponse> getComments(UUID requesterPublicId, UUID projectPublicId, UUID taskPublicId) {
		validatePermission(requesterPublicId, projectPublicId);

		Task task = taskRepository.findByPublicIdAndProjectPublicId(taskPublicId, projectPublicId)
								  .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		return taskCommentRepository.findTopLevelCommentsWithChildren(task.getId())
									.stream()
									.map(CommentResponse::from)
									.toList();
	}

	/**
	 * 댓글 수정
	 * - 작성자 본인만 가능
	 */
	public void updateComment(UUID requesterPublicId, Long commentId, String newContent) {
		TaskComment comment = taskCommentRepository.findByIdWithAuthor(commentId)
												   .orElseThrow(() -> new CustomException(COMMENT_NOT_FOUND));

		validateIsAuthor(requesterPublicId, comment);

		comment.updateContent(newContent);
		log.info("댓글 수정 - commentId={}, authorId={}", commentId, requesterPublicId);
	}

	/**
	 * 댓글 삭제
	 * - 작성자 본인만 가능
	 * - 대댓글 CascadeType.REMOVE
	 */
	public void deleteComment(UUID requesterPublicId, Long commentId) {
		TaskComment comment = taskCommentRepository.findByIdWithAuthorAndChildren(commentId)
												   .orElseThrow(() -> new CustomException(COMMENT_NOT_FOUND));

		validateIsAuthor(requesterPublicId, comment);

		taskCommentRepository.delete(comment);
		log.info("댓글 삭제 - commentId={}, authorId={}", commentId, requesterPublicId);
	}

	// 헬퍼 -------------------------
	/**
	 * 권한 검증 후 User 반환
	 * - 비공개 프로젝트면 해당 프로젝트에 속한 멤버만 가능
	 * - 공개 프로젝트면 워크스페이스 멤버면 가능
	 */
	private User validatePermission(UUID requesterPublicId, UUID projectPublicId) {
		Project project = projectRepository.findByPublicIdWithWorkspace(projectPublicId)
										   .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		if (project.isPrivate()) {
			return projectMemberRepository.findMemberWithUser(requesterPublicId, projectPublicId)
										  .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED))
										  .getUser();
		} else {
			UUID workspacePublicId = project.getWorkspace().getPublicId();
			return workspaceMemberRepository.findMemberWithUser(workspacePublicId, requesterPublicId)
											.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED))
											.getUser();
		}
	}

	private void validateIsAuthor(UUID requesterPublicId, TaskComment comment) {
		if (!comment.getAuthor().getPublicId().equals(requesterPublicId)) {
			throw new CustomException(COMMENT_ACCESS_DENIED);
		}
	}

	/**
	 * [댓글 알림 발송 원칙]
	 * 1. 요청자 제외: 댓글을 작성한 본인에게는 알림을 보내지 않음.
	 * 2. 최상위 댓글: 업무의 관계자(Reporter, Assignee)에게 새로운 의견이 등록되었음을 알림.
	 * 3. 대댓글(답글):
	 * - 원본 상위 댓글 작성자에게 본인 글에 답글이 달렸음을 알림.
	 * - 업무 관계자(Reporter, Assignee)에게도 대화 흐름 공유를 위해 함께 알림.
	 * 4. 중복 방지: 동일 인물이 여러 역할(상위 댓글 작성자이자 담당자 등)을 겸할 경우 알림은 한 번만 발송.
	 */
	private void sendNotifications(Task task, UUID projectPublicId, User author, TaskComment comment) {
		User assignee = task.getAssignee();
		User reporter = task.getReporter();

		Set<Long> recipientIds = new HashSet<>();

		recipientIds.add(reporter.getId());
		recipientIds.add(assignee.getId());

		TaskComment parentComment = comment.getParentComment();

		if (parentComment != null) {
			recipientIds.add(parentComment.getAuthor().getId());
		}


		String msg;
		NotificationType type;

		if (parentComment == null) {
			msg = String.format("'%s' 업무에 새로운 댓글이 등록되었습니다.", task.getName());
			type = NotificationType.COMMENT_ADDED;
		} else {
			msg = String.format("'%s' 업무의 댓글에 답글이 달렸습니다.", task.getName());
			type = NotificationType.COMMENT_REPLY_ADDED;
		}

		// 요청자 필터링 및 발송
		recipientIds.stream()
					.filter(id -> !id.equals(author.getId()))
					.forEach(id -> eventPublisher.publishEvent(new NotificationEvent(
							id, type, ReferenceType.TASK, task.getPublicId(), projectPublicId, msg
					)));
	}
}