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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.CreateProjectRequest;
import xyz.letzcollab.backend.dto.task.*;
import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.TaskRepository;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@DisplayName("TaskService 통합 테스트")
class TaskServiceTest {

	@MockitoBean
	ApplicationEventPublisher eventPublisher;

	@Autowired
	EntityManager em;

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
	TaskRepository taskRepository;

	private User leader;       // ADMIN
	private User member;       // MEMBER
	private User viewer;       // VIEWER
	private User wsOnlyMember; // 워크스페이스 멤버, 프로젝트 비소속
	private User outsider;     // 워크스페이스/프로젝트 모두 비소속

	private Workspace workspace;
	private UUID projectId;
	private UUID privateProjectId;

	@BeforeEach
	void setUp() {
		leader = saveUser("leader@test.com", "리더");
		member = saveUser("member@test.com", "멤버");
		viewer = saveUser("viewer@test.com", "조회자");
		wsOnlyMember = saveUser("wsonly@test.com", "워크스페이스멤버");
		outsider = saveUser("outsider@test.com", "외부인");

		UUID workspaceId = workspaceService.createWorkspace(leader.getPublicId(), "테스트 워크스페이스", "CTO");
		workspace = workspaceRepository.findWorkspaceByPublicIdWithOwner(workspaceId).orElseThrow();

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(member, workspace, "개발자"));
		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(viewer, workspace, "디자이너"));
		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(wsOnlyMember, workspace, "기획자"));

		// 공개 프로젝트
		projectId = projectService.createProject(leader.getPublicId(), workspaceId,
				new CreateProjectRequest("테스트 프로젝트", "설명", ProjectStatus.ACTIVE,
						LocalDate.now(), LocalDate.now().plusDays(30), false, "리더"));

		projectMemberService.addMember(leader.getPublicId(), workspaceId, projectId,
				new AddMemberRequest(member.getPublicId(), ProjectRole.MEMBER, "개발자"));
		projectMemberService.addMember(leader.getPublicId(), workspaceId, projectId,
				new AddMemberRequest(viewer.getPublicId(), ProjectRole.VIEWER, "디자이너"));

		// 비공개 프로젝트
		privateProjectId = projectService.createProject(leader.getPublicId(), workspaceId,
				new CreateProjectRequest("비공개 프로젝트", "설명", ProjectStatus.ACTIVE,
						LocalDate.now(), LocalDate.now().plusDays(30), true, "리더"));

		projectMemberService.addMember(leader.getPublicId(), workspaceId, privateProjectId,
				new AddMemberRequest(member.getPublicId(), ProjectRole.MEMBER, "개발자"));
	}


	@Nested
	@DisplayName("업무 생성")
	class CreateTask {

		@Test
		@DisplayName("ADMIN이 업무를 생성하면 DB에 저장된다")
		void adminCanCreateTask() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			assertThat(taskRepository.findTaskDetailsByPublicId(taskId)).isPresent();
		}

		@Test
		@DisplayName("MEMBER가 업무를 생성하면 DB에 저장된다")
		void memberCanCreateTask() {
			UUID taskId = createSampleTask(member.getPublicId(), member.getPublicId(), projectId);
			assertThat(taskRepository.findTaskDetailsByPublicId(taskId)).isPresent();
		}

		@Test
		@DisplayName("생성된 업무의 초기 상태는 TODO이다")
		void initialStatusIsTodo() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			Task task = taskRepository.findTaskDetailsByPublicId(taskId).orElseThrow();
			assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
		}

		@Test
		@DisplayName("reporter는 요청자로 고정된다")
		void reporterIsFixedToRequester() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			Task task = taskRepository.findTaskDetailsByPublicId(taskId).orElseThrow();
			assertThat(task.getReporter().getPublicId()).isEqualTo(leader.getPublicId());
		}

		@Test
		@DisplayName("자기 자신에게 업무를 할당할 수 있다")
		void canAssignToSelf() {
			UUID taskId = createSampleTask(member.getPublicId(), member.getPublicId(), projectId);
			Task task = taskRepository.findTaskDetailsByPublicId(taskId).orElseThrow();
			assertThat(task.getAssignee().getPublicId()).isEqualTo(member.getPublicId());
		}

		@Test
		@DisplayName("VIEWER는 업무 생성 불가 - INSUFFICIENT_PERMISSION")
		void viewerCannotCreateTask() {
			assertThatThrownBy(() -> createSampleTask(viewer.getPublicId(), viewer.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("VIEWER에게 업무를 할당하면 CANNOT_ASSIGN_TASK_TO_VIEWER")
		void cannotAssignToViewer() {
			assertThatThrownBy(() -> createSampleTask(leader.getPublicId(), viewer.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(CANNOT_ASSIGN_TASK_TO_VIEWER);
		}

		@Test
		@DisplayName("MEMBER가 ADMIN에게 할당하면 INSUFFICIENT_PERMISSION")
		void memberCannotAssignToAdmin() {
			assertThatThrownBy(() -> createSampleTask(member.getPublicId(), leader.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("프로젝트 비소속 사용자가 생성 시도하면 TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void outsiderCannotCreateTask() {
			assertThatThrownBy(() -> createSampleTask(outsider.getPublicId(), member.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("하위 업무 생성")
	class CreateSubTask {

		@Test
		@DisplayName("부모 업무의 reporter가 하위 업무를 생성할 수 있다")
		void parentReporterCanCreateSubTask() {
			UUID parentId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			UUID subTaskId = taskService.createSubTask(
					leader.getPublicId(), projectId, parentId,
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null));

			Task subTask = taskRepository.findTaskDetailsByPublicId(subTaskId).orElseThrow();
			assertThat(subTask.getParentTask().getPublicId()).isEqualTo(parentId);
		}

		@Test
		@DisplayName("부모 업무의 assignee가 하위 업무를 생성할 수 있다")
		void parentAssigneeCanCreateSubTask() {
			UUID parentId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			UUID subTaskId = taskService.createSubTask(
					member.getPublicId(), projectId, parentId,
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null));

			assertThat(taskRepository.findTaskDetailsByPublicId(subTaskId)).isPresent();
		}

		@Test
		@DisplayName("ADMIN은 모든 업무에 하위 업무를 생성할 수 있다")
		void adminCanCreateSubTask() {
			UUID parentId = createSampleTask(member.getPublicId(), member.getPublicId(), projectId);
			UUID subTaskId = taskService.createSubTask(
					leader.getPublicId(), projectId, parentId,
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null));

			assertThat(taskRepository.findTaskDetailsByPublicId(subTaskId)).isPresent();
		}

		@Test
		@DisplayName("부모 업무와 무관한 MEMBER가 하위 업무 생성 시도하면 INSUFFICIENT_PERMISSION")
		void unrelatedMemberCannotCreateSubTask() {
			User otherMember = saveUser("other@test.com", "다른멤버");
			workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(otherMember, workspace, "개발자"));
			projectMemberService.addMember(
					leader.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(otherMember.getPublicId(), ProjectRole.MEMBER, "개발자"));

			UUID parentId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			assertThatThrownBy(() -> taskService.createSubTask(
					otherMember.getPublicId(), projectId, parentId,
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("존재하지 않는 부모 업무 ID로 생성 시도하면 TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void invalidParentTaskThrows() {
			assertThatThrownBy(() -> taskService.createSubTask(
					leader.getPublicId(), projectId, UUID.randomUUID(),
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("업무 목록 조회")
	class GetTasks {

		@Test
		@DisplayName("프로젝트 멤버는 업무 목록을 조회할 수 있다")
		void memberCanGetTasks() {
			createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			createSampleTask(leader.getPublicId(), leader.getPublicId(), projectId);

			Page<TaskResponse> result = taskService.getTasks(
					member.getPublicId(), projectId, emptySearchCond(), PageRequest.of(0, 10));
			assertThat(result.getTotalElements()).isEqualTo(2);
		}

		@Test
		@DisplayName("공개 프로젝트는 워크스페이스 멤버도 조회할 수 있다")
		void wsMemberCanGetTasksOnPublicProject() {
			createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			Page<TaskResponse> result = taskService.getTasks(
					wsOnlyMember.getPublicId(), projectId, emptySearchCond(), PageRequest.of(0, 10));
			assertThat(result.getTotalElements()).isEqualTo(1);
		}

		@Test
		@DisplayName("비공개 프로젝트는 프로젝트 멤버가 아닌 워크스페이스 멤버는 조회 불가 - TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void wsMemberCannotGetTasksOnPrivateProject() {
			assertThatThrownBy(() ->
					taskService.getTasks(wsOnlyMember.getPublicId(), privateProjectId, emptySearchCond(), PageRequest.of(0, 10)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("워크스페이스 비소속 사용자가 조회 시도하면 TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void outsiderCannotGetTasks() {
			assertThatThrownBy(() ->
					taskService.getTasks(outsider.getPublicId(), projectId, emptySearchCond(), PageRequest.of(0, 10)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("status 필터로 조회 시 해당 상태의 업무만 반환된다")
		void filterByStatus() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			taskService.updateTask(
					leader.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));

			Page<TaskResponse> result = taskService.getTasks(
					member.getPublicId(), projectId,
					new TaskSearchCond(TaskStatus.DONE, null, null, null, null),
					PageRequest.of(0, 10));
			assertThat(result).allMatch(t -> t.status() == TaskStatus.DONE);
		}
	}


	@Nested
	@DisplayName("업무 상세 조회")
	class GetTaskDetails {

		@Test
		@DisplayName("프로젝트 멤버는 업무 상세를 조회할 수 있다")
		void memberCanGetDetails() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			TaskDetailsResponse response = taskService.getTaskDetails(member.getPublicId(), projectId, taskId);

			assertThat(response.publicId()).isEqualTo(taskId);
			assertThat(response.assigneePublicId()).isEqualTo(member.getPublicId());
			assertThat(response.reporterPublicId()).isEqualTo(leader.getPublicId());
		}

		@Test
		@DisplayName("존재하지 않는 업무 조회 시 TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void notFoundThrows() {
			assertThatThrownBy(() ->
					taskService.getTaskDetails(leader.getPublicId(), projectId, UUID.randomUUID()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("업무 수정")
	class UpdateTask {

		@Test
		@DisplayName("ADMIN은 모든 필드를 수정할 수 있다")
		void adminCanUpdateAllFields() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			taskService.updateTask(leader.getPublicId(), projectId, taskId,
					new UpdateTaskRequest("수정된 이름", "수정된 설명", TaskStatus.IN_PROGRESS, null, TaskPriority.HIGH, null));

			Task updated = taskRepository.findTaskDetailsByPublicId(taskId).orElseThrow();
			assertThat(updated.getName()).isEqualTo("수정된 이름");
			assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
			assertThat(updated.getPriority()).isEqualTo(TaskPriority.HIGH);
		}

		@Test
		@DisplayName("reporter(MEMBER)도 모든 필드를 수정할 수 있다")
		void reporterCanUpdateAllFields() {
			UUID taskId = createSampleTask(member.getPublicId(), member.getPublicId(), projectId);

			taskService.updateTask(member.getPublicId(), projectId, taskId,
					new UpdateTaskRequest("수정된 이름", null, TaskStatus.IN_PROGRESS, null, TaskPriority.HIGH, null));

			Task updated = taskRepository.findTaskDetailsByPublicId(taskId).orElseThrow();
			assertThat(updated.getName()).isEqualTo("수정된 이름");
		}

		@Test
		@DisplayName("assignee는 허용된 상태로만 변경할 수 있다 (TODO → IN_PROGRESS)")
		void assigneeCanChangeStatusWithinAllowed() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			taskService.updateTask(member.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));

			Task updated = taskRepository.findTaskDetailsByPublicId(taskId).orElseThrow();
			assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
		}

		@Test
		@DisplayName("assignee가 허용 범위 밖 상태(DONE)로 변경 시도하면 INSUFFICIENT_PERMISSION")
		void assigneeCannotChangeStatusToDone() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			assertThatThrownBy(() -> taskService.updateTask(member.getPublicId(), projectId, taskId,
					new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("assignee가 이름 수정 시도하면 INSUFFICIENT_PERMISSION")
		void assigneeCannotUpdateName() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			assertThatThrownBy(() -> taskService.updateTask(member.getPublicId(), projectId, taskId,
					new UpdateTaskRequest("변경 시도", null, null, null, null, null)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("CANCELLED로 변경하면 하위 업무도 전부 CANCELLED 된다")
		void cancelledCascadesToSubTasks() {
			UUID parentId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			UUID subTaskId = taskService.createSubTask(
					leader.getPublicId(), projectId, parentId,
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null));

			em.flush();
			em.clear();

			taskService.updateTask(leader.getPublicId(), projectId, parentId,
					new UpdateTaskRequest(null, null, TaskStatus.CANCELLED, null, null, null));


			Task parent = taskRepository.findTaskDetailsByPublicId(parentId).orElseThrow();
			Task subTask = taskRepository.findTaskDetailsByPublicId(subTaskId).orElseThrow();
			assertThat(parent.getStatus()).isEqualTo(TaskStatus.CANCELLED);
			assertThat(subTask.getStatus()).isEqualTo(TaskStatus.CANCELLED);
		}

		@Test
		@DisplayName("프로젝트 비소속 사용자가 수정 시도하면 TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void outsiderCannotUpdate() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			assertThatThrownBy(() -> taskService.updateTask(outsider.getPublicId(), projectId, taskId,
					new UpdateTaskRequest("수정", null, null, null, null, null)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}

	// ── 업무 삭제 ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("업무 삭제 (Soft Delete)")
	class DeleteTask {

		@Test
		@DisplayName("삭제 후 조회되지 않는다")
		void softDeleteHidesTask() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			taskService.deleteTask(leader.getPublicId(), projectId, taskId);

			assertThat(taskRepository.findTaskDetailsByPublicId(taskId)).isEmpty();
		}

		@Test
		@DisplayName("삭제 시 하위 업무도 함께 삭제된다")
		void deleteTaskCascadesToSubTasks() {
			UUID parentId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);
			UUID subTaskId = taskService.createSubTask(
					leader.getPublicId(), projectId, parentId,
					new CreateTaskRequest("하위 업무", null, member.getPublicId(), TaskPriority.LOW, null));

			em.flush();
			em.clear();

			taskService.deleteTask(leader.getPublicId(), projectId, parentId);

			assertThat(taskRepository.findTaskDetailsByPublicId(parentId)).isEmpty();
			assertThat(taskRepository.findTaskDetailsByPublicId(subTaskId)).isEmpty();
		}

		@Test
		@DisplayName("MEMBER는 본인이 reporter인 업무만 삭제 가능")
		void memberCanDeleteOwnTask() {
			UUID taskId = createSampleTask(member.getPublicId(), member.getPublicId(), projectId);

			taskService.deleteTask(member.getPublicId(), projectId, taskId);

			assertThat(taskRepository.findTaskDetailsByPublicId(taskId)).isEmpty();
		}

		@Test
		@DisplayName("MEMBER가 타인의 업무 삭제 시도하면 INSUFFICIENT_PERMISSION")
		void memberCannotDeleteOthersTask() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			assertThatThrownBy(() -> taskService.deleteTask(member.getPublicId(), projectId, taskId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("프로젝트 비소속 사용자가 삭제 시도하면 TASK_NOT_FOUND_OR_ACCESS_DENIED")
		void outsiderCannotDelete() {
			UUID taskId = createSampleTask(leader.getPublicId(), member.getPublicId(), projectId);

			assertThatThrownBy(() -> taskService.deleteTask(outsider.getPublicId(), projectId, taskId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(TASK_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	// 헬퍼
	private UUID createSampleTask(UUID reporterPublicId, UUID assigneePublicId, UUID targetProjectId) {
		return taskService.createTask(reporterPublicId, targetProjectId,
				new CreateTaskRequest("샘플 업무", "설명", assigneePublicId, TaskPriority.MEDIUM,
						LocalDate.now().plusDays(7)));
	}

	private TaskSearchCond emptySearchCond() {
		return new TaskSearchCond(null, null, null, null, null);
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "pwd1234!?", null));
	}
}