package xyz.letzcollab.backend.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.CreateProjectRequest;
import xyz.letzcollab.backend.dto.task.CreateTaskRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.repository.TaskCommentRepository;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;
import xyz.letzcollab.backend.service.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@RecordApplicationEvents
@DisplayName("TaskCommentService 알림 이벤트 통합 테스트")
class TaskCommentServiceNotificationTest {

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

	@Autowired UserRepository userRepository;
	@Autowired WorkspaceRepository workspaceRepository;
	@Autowired WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired TaskCommentRepository taskCommentRepository;

	@Autowired ApplicationEvents events;

	private User reporter;
	private User assignee;
	private User commenter;
	private UUID projectId;
	private UUID taskId;

	@BeforeEach
	void setUp() {
		User admin = saveUser("admin@test.com", "관리자");
		reporter = saveUser("reporter@test.com", "보고자");
		assignee = saveUser("assignee@test.com", "담당자");
		commenter = saveUser("commenter@test.com", "댓글 작성자");

		workspaceService.createWorkspace(admin.getPublicId(), "테스트 워크스페이스", "CTO");
		Workspace workspace = workspaceRepository.findAll().stream()
												 .filter(w -> w.getName().equals("테스트 워크스페이스"))
												 .findFirst().orElseThrow();

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(reporter, workspace, "개발자"));
		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(assignee, workspace, "개발자"));
		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(commenter, workspace, "개발자"));

		projectId = projectService.createProject(admin.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest("테스트 프로젝트", null, ProjectStatus.ACTIVE, null, null, false, null));

		projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(reporter.getPublicId(), ProjectRole.MEMBER, null));
		projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(assignee.getPublicId(), ProjectRole.MEMBER, null));
		projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(commenter.getPublicId(), ProjectRole.MEMBER, null));

		taskId = taskService.createTask(reporter.getPublicId(), projectId,
				new CreateTaskRequest("테스트 업무", null, assignee.getPublicId(), TaskPriority.HIGH, null));

		events.clear();
	}

	@Nested
	@DisplayName("최상위 댓글 알림")
	class TopLevelComment {

		@Test
		@DisplayName("제3자가 댓글을 달면 reporter와 assignee에게 알림이 발행된다")
		void thirdPartyComments() {
			taskCommentService.createComment(commenter.getPublicId(), projectId, taskId, "댓글입니다", null);

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(2);

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.COMMENT_ADDED);
				assertThat(e.recipientId()).isEqualTo(reporter.getId());
			});

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.COMMENT_ADDED);
				assertThat(e.recipientId()).isEqualTo(assignee.getId());
			});
		}

		@Test
		@DisplayName("assignee가 댓글을 달면 reporter에게만 알림이 발행된다 (본인 제외)")
		void assigneeComments() {
			taskCommentService.createComment(assignee.getPublicId(), projectId, taskId, "댓글입니다", null);

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().recipientId()).isEqualTo(reporter.getId());
		}

		@Test
		@DisplayName("reporter와 assignee가 같은 경우, 댓글 작성자가 제3자이면 1건만 발행된다")
		void reporterIsAssignee() {
			// reporter == assignee인 업무 생성
			UUID selfTaskId = taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("셀프 업무", null, reporter.getPublicId(), TaskPriority.HIGH, null));
			events.clear();

			taskCommentService.createComment(commenter.getPublicId(), projectId, selfTaskId, "댓글입니다", null);

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().recipientId()).isEqualTo(reporter.getId());
		}
	}

	@Nested
	@DisplayName("대댓글 알림")
	class ReplyComment {

		@Test
		@DisplayName("대댓글을 달면 원본 댓글 작성자에게도 알림이 발행된다")
		void replyNotifiesParentAuthor() {
			taskCommentService.createComment(commenter.getPublicId(), projectId, taskId, "원본 댓글", null);
			events.clear();

			Long parentCommentId = taskCommentRepository.findTopLevelCommentsWithChildren(
					getTaskInternalId()).getFirst().getId();

			taskCommentService.createComment(assignee.getPublicId(), projectId, taskId, "답글", parentCommentId);

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();

			// reporter, commenter(원본 댓글 작성자)에게 알림 (assignee 본인은 제외)
			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.COMMENT_REPLY_ADDED);
				assertThat(e.recipientId()).isEqualTo(commenter.getId());
			});

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.recipientId()).isEqualTo(reporter.getId());
			});
		}

		@Test
		@DisplayName("원본 댓글 작성자가 assignee와 동일인물이면 중복 알림이 발행되지 않는다")
		void parentAuthorIsAssignee() {
			// assignee가 원댓글 작성
			taskCommentService.createComment(assignee.getPublicId(), projectId, taskId, "원본 댓글", null);
			events.clear();

			Long parentCommentId = taskCommentRepository.findTopLevelCommentsWithChildren(
					getTaskInternalId()).getFirst().getId();

			// commenter가 대댓글 작성
			taskCommentService.createComment(commenter.getPublicId(), projectId, taskId, "답글", parentCommentId);

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();

			// assignee(=원본 댓글 작성자)에게 알림 - 중복 없이 1건
			long assigneeNotifications = fired.stream()
											  .filter(e -> e.recipientId().equals(assignee.getId()))
											  .count();
			assertThat(assigneeNotifications).isEqualTo(1);
		}
	}

	// 헬퍼
	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "password1234!", null));
	}

	private Long getTaskInternalId() {
		return taskCommentRepository.findAll().stream()
									.findFirst()
									.map(c -> c.getTask().getId())
									.orElseThrow();
	}
}
