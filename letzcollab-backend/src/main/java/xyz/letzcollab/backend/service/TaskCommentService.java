package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.comment.CommentResponse;
import xyz.letzcollab.backend.dto.comment.CreateCommentRequest;
import xyz.letzcollab.backend.dto.comment.UpdateCommentRequest;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.TaskComment;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.ProjectMemberRepository;
import xyz.letzcollab.backend.repository.ProjectRepository;
import xyz.letzcollab.backend.repository.TaskCommentRepository;
import xyz.letzcollab.backend.repository.TaskRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;

import java.util.List;
import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskCommentService {

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

		Task task = taskRepository.findByPublicIdAndProjectPublicId(taskPublicId, projectPublicId)
								  .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		TaskComment comment;

		if (parentCommentId != null) {
			TaskComment parentComment = taskCommentRepository.findById(parentCommentId)
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
}