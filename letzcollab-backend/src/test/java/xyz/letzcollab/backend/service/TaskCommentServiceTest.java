package xyz.letzcollab.backend.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.comment.CommentResponse;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.CreateProjectRequest;
import xyz.letzcollab.backend.dto.task.CreateTaskRequest;
import xyz.letzcollab.backend.entity.TaskComment;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@DisplayName("TaskCommentService 통합 테스트")
class TaskCommentServiceTest {

	@MockitoBean
	ApplicationEventPublisher eventPublisher;

	@Autowired
	EntityManager em;

	@Autowired
	TaskCommentService taskCommentService;
	@Autowired
	TaskService taskService;
	@Autowired
	ProjectService projectService;
	@Autowired
	ProjectMemberService projectMemberService;
	@Autowired
	WorkspaceService workspaceService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	WorkspaceRepository workspaceRepository;
	@Autowired
	WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	TaskCommentRepository taskCommentRepository;
	@Autowired
	TaskRepository taskRepository;


	private User owner;
	private User member;    // 프로젝트 멤버 (댓글 작성 가능)
	private User otherMember; // 또 다른 프로젝트 멤버
	private User wsOutsider;  // 워크스페이스 멤버이지만 비공개 프로젝트에는 비소속
	private User stranger;    // 워크스페이스 비소속 외부인

	private UUID projectPublicId;       // 비공개 프로젝트
	private UUID publicProjectPublicId; // 공개 프로젝트
	private UUID taskPublicId;

	@BeforeEach
	void setUp() {
		owner = saveUser("owner@test.com", "소유자");
		member = saveUser("member@test.com", "일반 멤버");
		otherMember = saveUser("other@test.com", "다른 멤버");
		wsOutsider = saveUser("outsider@test.com", "프로젝트 외부인");
		stranger = saveUser("stranger@test.com", "워크스페이스 외부인");

		workspaceService.createWorkspace(owner.getPublicId(), "테스트 워크스페이스", "CTO");
		Workspace workspace = workspaceRepository.findAll().stream()
												 .filter(w -> w.getName().equals("테스트 워크스페이스"))
												 .findFirst().orElseThrow();

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(member, workspace, "개발자"));
		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(otherMember, workspace, "디자이너"));
		workspaceMemberRepository.saveAndFlush(WorkspaceMember.createGeneralMember(wsOutsider, workspace, "기획자"));

		// 비공개 프로젝트
		projectPublicId = projectService.createProject(
				owner.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest("비공개 프로젝트", null, ProjectStatus.ACTIVE, null, null, true, null)
		);
		projectMemberService.addMember(owner.getPublicId(), workspace.getPublicId(), projectPublicId,
				new AddMemberRequest(member.getPublicId(), ProjectRole.MEMBER, null));
		projectMemberService.addMember(owner.getPublicId(), workspace.getPublicId(), projectPublicId,
				new AddMemberRequest(otherMember.getPublicId(), ProjectRole.MEMBER, null));

		// 공개 프로젝트
		publicProjectPublicId = projectService.createProject(
				owner.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest("공개 프로젝트", null, ProjectStatus.ACTIVE, null, null, false, null)
		);

		// 업무 생성 (비공개 프로젝트)
		taskPublicId = taskService.createTask(
				member.getPublicId(), projectPublicId,
				new CreateTaskRequest("테스트 업무", null, member.getPublicId(), TaskPriority.MEDIUM, null)
		);
	}

	@Nested
	@DisplayName("댓글 생성")
	class CreateComment {

		@Test
		@DisplayName("프로젝트 멤버가 댓글을 작성하면 DB에 저장된다")
		void memberCanCreateComment() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "첫 댓글입니다", null);

			List<TaskComment> comments = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId());
			assertThat(comments).hasSize(1);
			assertThat(comments.getFirst().getContent()).isEqualTo("첫 댓글입니다");
		}

		@Test
		@DisplayName("공개 프로젝트는 워크스페이스 멤버면 댓글 작성 가능")
		void wsMemberCanCommentOnPublicProject() {
			UUID publicTaskId = taskService.createTask(
					owner.getPublicId(), publicProjectPublicId,
					new CreateTaskRequest("공개 업무", null, owner.getPublicId(), TaskPriority.LOW, null)
			);

			// wsOutsider는 공개 프로젝트 멤버는 아니지만 워크스페이스 멤버이므로 가능
			taskCommentService.createComment(wsOutsider.getPublicId(), publicProjectPublicId, publicTaskId, "공개 댓글", null);

			List<TaskComment> comments = taskCommentRepository.findTopLevelCommentsWithChildren(
					taskRepository.findByPublicIdAndProjectPublicId(publicTaskId, publicProjectPublicId).orElseThrow()
								  .getId()
			);
			assertThat(comments).hasSize(1);
		}

		@Test
		@DisplayName("비공개 프로젝트 비소속 멤버는 댓글 작성 불가 → TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void nonProjectMemberCannotComment() {
			assertThatThrownBy(() ->
					taskCommentService.createComment(wsOutsider.getPublicId(), projectPublicId, taskPublicId, "댓글", null))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("워크스페이스 비소속 외부인은 댓글 작성 불가 → TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void strangerCannotComment() {
			assertThatThrownBy(() ->
					taskCommentService.createComment(stranger.getPublicId(), projectPublicId, taskPublicId, "댓글", null))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("대댓글을 작성하면 parentComment가 연결된다")
		void canCreateReply() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "부모 댓글", null);
			Long parentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();

			taskCommentService.createComment(otherMember.getPublicId(), projectPublicId, taskPublicId, "대댓글입니다", parentId);

			em.flush();
			em.clear();

			List<TaskComment> topLevel = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId());
			assertThat(topLevel.getFirst().getChildComments()).hasSize(1);
			assertThat(topLevel.getFirst().getChildComments().getFirst().getContent()).isEqualTo("대댓글입니다");
		}

		@Test
		@DisplayName("대댓글에 대댓글을 달면 COMMENT_REPLY_DEPTH_EXCEEDED")
		void cannotReplyToReply() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "부모 댓글", null);
			Long parentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();

			taskCommentService.createComment(otherMember.getPublicId(), projectPublicId, taskPublicId, "대댓글", parentId);

			em.flush();
			em.clear();

			Long replyId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId())
												.getFirst().getChildComments().getFirst().getId();

			assertThatThrownBy(() ->
					taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "대대댓글", replyId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(COMMENT_REPLY_DEPTH_EXCEEDED);
		}

		@Test
		@DisplayName("존재하지 않는 부모 댓글 ID로 대댓글 작성 시 COMMENT_NOT_FOUND")
		void parentCommentNotFound() {
			assertThatThrownBy(() ->
					taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "대댓글", Long.MAX_VALUE))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(COMMENT_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("댓글 목록 조회")
	class GetComments {

		@Test
		@DisplayName("댓글 목록을 작성 순서대로 반환한다")
		void returnsCommentsInOrder() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "첫 번째 댓글", null);
			taskCommentService.createComment(otherMember.getPublicId(), projectPublicId, taskPublicId, "두 번째 댓글", null);

			List<CommentResponse> result = taskCommentService.getComments(member.getPublicId(), projectPublicId, taskPublicId);

			assertThat(result).hasSize(2);
			assertThat(result.get(0).content()).isEqualTo("첫 번째 댓글");
			assertThat(result.get(1).content()).isEqualTo("두 번째 댓글");
		}

		@Test
		@DisplayName("댓글 응답에 대댓글이 포함된다")
		void repliesAreNested() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "부모 댓글", null);
			Long parentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();
			taskCommentService.createComment(otherMember.getPublicId(), projectPublicId, taskPublicId, "대댓글", parentId);

			em.flush();
			em.clear();

			List<CommentResponse> result = taskCommentService.getComments(member.getPublicId(), projectPublicId, taskPublicId);

			assertThat(result).hasSize(1);
			assertThat(result.getFirst().replies()).hasSize(1);
			assertThat(result.getFirst().replies().getFirst().content()).isEqualTo("대댓글");
		}

		@Test
		@DisplayName("댓글이 없으면 빈 목록을 반환한다")
		void returnsEmptyWhenNoComments() {
			List<CommentResponse> result = taskCommentService.getComments(member.getPublicId(), projectPublicId, taskPublicId);
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("비공개 프로젝트에 속한 멤버는 댓글 목록을 조회할 수 있다")
		void projectMemberCanReadComments() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "댓글", null);

			List<CommentResponse> result = taskCommentService.getComments(otherMember.getPublicId(), projectPublicId, taskPublicId);
			assertThat(result).hasSize(1);
		}

		@Test
		@DisplayName("비공개 프로젝트 비소속 멤버는 댓글 조회 불가 → TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void nonProjectMemberCannotRead() {
			assertThatThrownBy(() ->
					taskCommentService.getComments(wsOutsider.getPublicId(), projectPublicId, taskPublicId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("공개 프로젝트는 워크스페이스 멤버면 댓글 목록 조회 가능")
		void wsMemberCanReadCommentsOnPublicProject() {
			UUID publicTaskId = taskService.createTask(
					owner.getPublicId(), publicProjectPublicId,
					new CreateTaskRequest("공개 업무", null, owner.getPublicId(), TaskPriority.LOW, null)
			);
			taskCommentService.createComment(owner.getPublicId(), publicProjectPublicId, publicTaskId, "공개 댓글", null);

			// wsOutsider는 공개 프로젝트 멤버는 아니지만 워크스페이스 멤버이므로 조회 가능
			List<CommentResponse> result = taskCommentService.getComments(wsOutsider.getPublicId(), publicProjectPublicId, publicTaskId);
			assertThat(result).hasSize(1);
		}

	}

	@Nested
	@DisplayName("댓글 수정")
	class UpdateComment {

		@Test
		@DisplayName("작성자가 댓글 내용을 수정하면 DB에 반영된다")
		void authorCanUpdate() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "원본 댓글", null);
			Long commentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();

			taskCommentService.updateComment(member.getPublicId(), commentId, "수정된 댓글");

			TaskComment updated = taskCommentRepository.findByIdWithAuthor(commentId).orElseThrow();
			assertThat(updated.getContent()).isEqualTo("수정된 댓글");
		}

		@Test
		@DisplayName("작성자가 아닌 멤버가 수정 시도 시 COMMENT_ACCESS_DENIED")
		void nonAuthorCannotUpdate() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "원본 댓글", null);
			Long commentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();

			assertThatThrownBy(() ->
					taskCommentService.updateComment(otherMember.getPublicId(), commentId, "수정 시도"))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(COMMENT_ACCESS_DENIED);
		}

		@Test
		@DisplayName("존재하지 않는 댓글 수정 시 COMMENT_NOT_FOUND")
		void commentNotFound() {
			assertThatThrownBy(() ->
					taskCommentService.updateComment(member.getPublicId(), Long.MAX_VALUE, "수정"))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(COMMENT_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("댓글 삭제")
	class DeleteComment {

		@Test
		@DisplayName("작성자가 댓글을 삭제하면 DB에서 제거된다")
		void authorCanDelete() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "삭제할 댓글", null);
			Long commentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();

			taskCommentService.deleteComment(member.getPublicId(), commentId);

			assertThat(taskCommentRepository.findById(commentId)).isEmpty();
		}

		@Test
		@DisplayName("부모 댓글 삭제 시 대댓글도 함께 삭제된다")
		void deletingParentDeletesReplies() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "부모 댓글", null);
			Long parentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();
			taskCommentService.createComment(otherMember.getPublicId(), projectPublicId, taskPublicId, "대댓글", parentId);

			em.flush();
			em.clear();

			Long replyId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId())
												.getFirst().getChildComments().getFirst().getId();

			taskCommentService.deleteComment(member.getPublicId(), parentId);

			assertThat(taskCommentRepository.findById(parentId)).isEmpty();
			assertThat(taskCommentRepository.findById(replyId)).isEmpty();
		}

		@Test
		@DisplayName("작성자가 아닌 멤버가 삭제 시도 시 COMMENT_ACCESS_DENIED")
		void nonAuthorCannotDelete() {
			taskCommentService.createComment(member.getPublicId(), projectPublicId, taskPublicId, "삭제할 댓글", null);
			Long commentId = taskCommentRepository.findTopLevelCommentsWithChildren(findTaskId()).getFirst().getId();

			assertThatThrownBy(() ->
					taskCommentService.deleteComment(otherMember.getPublicId(), commentId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(COMMENT_ACCESS_DENIED);
		}

		@Test
		@DisplayName("존재하지 않는 댓글 삭제 시 COMMENT_NOT_FOUND")
		void commentNotFound() {
			assertThatThrownBy(() ->
					taskCommentService.deleteComment(member.getPublicId(), Long.MAX_VALUE))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(COMMENT_NOT_FOUND);
		}
	}

	// 헬퍼 -------------------------

	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "password1234!", null));
	}

	private Long findTaskId() {
		return taskCommentRepository.findAll().stream()
									.findFirst()
									.map(c -> c.getTask().getId())
									.orElseThrow();
	}
}