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
import xyz.letzcollab.backend.dto.task.UpdateTaskRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.*;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;
import xyz.letzcollab.backend.service.ProjectMemberService;
import xyz.letzcollab.backend.service.ProjectService;
import xyz.letzcollab.backend.service.TaskService;
import xyz.letzcollab.backend.service.WorkspaceService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@RecordApplicationEvents
@DisplayName("TaskService 알림 이벤트 통합 테스트")
class TaskServiceNotificationTest {

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
	ApplicationEvents events;

	private User reporter;
	private User assignee;
	private User admin;
	private Workspace workspace;
	private UUID projectId;

	@BeforeEach
	void setUp() {
		reporter = saveUser("reporter@test.com", "보고자");
		assignee = saveUser("assignee@test.com", "담당자");
		admin = saveUser("admin@test.com", "관리자");

		workspaceService.createWorkspace(admin.getPublicId(), "테스트 워크스페이스", "CTO");
		workspace = workspaceRepository.findAll().stream()
									   .filter(w -> w.getName().equals("테스트 워크스페이스"))
									   .findFirst().orElseThrow();

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(reporter, workspace, "개발자"));
		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(assignee, workspace, "개발자"));

		projectId = projectService.createProject(admin.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest("테스트 프로젝트", null, ProjectStatus.ACTIVE, null, null, false, null));

		projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(reporter.getPublicId(), ProjectRole.MEMBER, null));
		projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(assignee.getPublicId(), ProjectRole.MEMBER, null));

		// setUp에서 발행된 이벤트 초기화
		events.clear();
	}

	@Nested
	@DisplayName("업무 생성 알림")
	class CreateTask {

		@Test
		@DisplayName("다른 사람에게 업무를 할당하면 TASK_ASSIGNED 이벤트가 발행된다")
		void taskAssignedToOther() {
			taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, assignee.getPublicId(), TaskPriority.HIGH, null));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().type()).isEqualTo(NotificationType.TASK_ASSIGNED);
			assertThat(fired.getFirst().recipientId()).isEqualTo(assignee.getId());
		}

		@Test
		@DisplayName("본인에게 업무를 할당하면 이벤트가 발행되지 않는다")
		void taskAssignedToSelf() {
			taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, reporter.getPublicId(), TaskPriority.HIGH, null));

			long count = events.stream(NotificationEvent.class).count();
			assertThat(count).isZero();
		}
	}

	@Nested
	@DisplayName("업무 상태 변경 알림")
	class UpdateTaskStatus {

		@Test
		@DisplayName("담당자가 상태를 변경하면 reporter에게 TASK_STATUS_CHANGED 이벤트가 발행된다")
		void assigneeChangesStatus() {
			UUID taskId = taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, assignee.getPublicId(), TaskPriority.HIGH, null));
			events.clear();

			taskService.updateTask(assignee.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().type()).isEqualTo(NotificationType.TASK_STATUS_CHANGED);
			assertThat(fired.getFirst().recipientId()).isEqualTo(reporter.getId());
		}

		@Test
		@DisplayName("reporter가 상태를 변경하면 assignee에게 TASK_STATUS_CHANGED 이벤트가 발행된다")
		void reporterChangesStatus() {
			UUID taskId = taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, assignee.getPublicId(), TaskPriority.HIGH, null));
			events.clear();

			taskService.updateTask(reporter.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().recipientId()).isEqualTo(assignee.getId());
		}

		@Test
		@DisplayName("reporter와 assignee가 동일인물이면 상태 변경 알림이 발행되지 않는다")
		void reporterIsAssignee() {
			UUID taskId = taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, reporter.getPublicId(), TaskPriority.HIGH, null));
			events.clear();

			taskService.updateTask(reporter.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));

			long count = events.stream(NotificationEvent.class).count();
			assertThat(count).isZero();
		}
	}

	@Nested
	@DisplayName("담당자 변경 알림")
	class ReassignTask {

		@Test
		@DisplayName("reporter가 담당자를 변경하면 기존 담당자에게 TASK_REASSIGNED, 새 담당자에게 TASK_ASSIGNED가 발행된다")
		void reporterReassigns() {
			User newAssignee = saveUser("new@test.com", "새 담당자");
			workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(newAssignee, workspace, "개발자"));
			projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(newAssignee.getPublicId(), ProjectRole.MEMBER, null));

			UUID taskId = taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, assignee.getPublicId(), TaskPriority.HIGH, null));
			events.clear();

			taskService.updateTask(reporter.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, null, newAssignee.getPublicId(), null, null));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.TASK_REASSIGNED);
				assertThat(e.recipientId()).isEqualTo(assignee.getId());
			});

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.TASK_ASSIGNED);
				assertThat(e.recipientId()).isEqualTo(newAssignee.getId());
			});
		}

		@Test
		@DisplayName("제3자의 ADMIN이 담당자를 변경하면 기존 담당자, 새 담당자, 보고자 모두에게 알림이 발행된다")
		void adminReassigns() {
			User newAssignee = saveUser("new@test.com", "새 담당자");
			workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(newAssignee, workspace, "개발자"));
			projectMemberService.addMember(admin.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(newAssignee.getPublicId(), ProjectRole.MEMBER, null));

			UUID taskId = taskService.createTask(reporter.getPublicId(), projectId,
					new CreateTaskRequest("API 설계", null, assignee.getPublicId(), TaskPriority.HIGH, null));
			events.clear();

			taskService.updateTask(admin.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, null, newAssignee.getPublicId(), null, null));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(3);

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.TASK_REASSIGNED);
				assertThat(e.recipientId()).isEqualTo(assignee.getId());
			});

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.TASK_ASSIGNED);
				assertThat(e.recipientId()).isEqualTo(newAssignee.getId());
			});

			assertThat(fired).anySatisfy(e -> {
				assertThat(e.type()).isEqualTo(NotificationType.TASK_REASSIGNED);
				assertThat(e.recipientId()).isEqualTo(reporter.getId());
			});
		}
	}

	// 헬퍼
	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "password1234!", null));
	}
}
