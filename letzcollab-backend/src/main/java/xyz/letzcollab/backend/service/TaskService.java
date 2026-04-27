package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.task.*;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ReferenceType;
import xyz.letzcollab.backend.entity.vo.TaskStatus;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.ProjectMemberRepository;
import xyz.letzcollab.backend.repository.ProjectRepository;
import xyz.letzcollab.backend.repository.TaskRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;

import java.util.*;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskService {

	private final ApplicationEventPublisher eventPublisher;

	private final TaskRepository taskRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectRepository projectRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	// assignee가 변경할 수 있는 상태 범위: TODO → IN_PROGRESS → IN_REVIEW
	private static final Set<TaskStatus> ASSIGNEE_ALLOWED_STATUSES =
			Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW);

	/**
	 * 최상위 업무 생성
	 * - VIEWER는 생성 불가
	 * - ADMIN: ADMIN/MEMBER 누구에게나 할당 가능
	 * - MEMBER: 본인 혹은 다른 MEMBER에게만 할당 가능
	 * - reporter는 요청자로 고정
	 * - 자기 자신에게 할당 허용 (reporter == assignee)
	 */
	public UUID createTask(UUID reporterPublicId, UUID projectPublicId, CreateTaskRequest req) {
		ProjectMember reporter = projectMemberRepository
				.findMemberWithUserAndProject(reporterPublicId, projectPublicId)
				.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		if (reporter.getRole() == ProjectRole.VIEWER) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}

		ProjectMember assigneeMember = getAssigneeMember(
				reporterPublicId, projectPublicId, req.assigneePublicId(), reporter);

		validateAssignPermission(reporter, assigneeMember, req.assigneePublicId());

		Project project = reporter.getProject();
		User reporterUser = reporter.getUser();
		User assigneeUser = assigneeMember.getUser();

		Task task = Task.createTask(
				project, req.name(), req.description(),
				assigneeUser, req.priority(), null, reporterUser, req.dueDate()
		);

		taskRepository.save(task);
		log.info("업무 생성 - taskName={}, projectId={}, reporterId={}, assigneeId={}",
				req.name(), projectPublicId, reporterPublicId, req.assigneePublicId());

		sendTaskAssignedNotification(reporterPublicId, assigneeUser, task, req.name(), projectPublicId);

		return task.getPublicId();
	}

	/**
	 * 하위 업무 생성
	 * - ADMIN 또는 부모 업무의 reporter/assignee만 생성 가능
	 * - 할당 권한은 최상위 업무와 동일
	 * - reporter는 요청자로 고정
	 */
	public UUID createSubTask(UUID reporterPublicId, UUID projectPublicId, UUID parentTaskPublicId, CreateTaskRequest req) {
		ProjectMember reporter = projectMemberRepository
				.findMemberWithUserAndProject(reporterPublicId, projectPublicId)
				.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		// 부모 업무 조회 — reporter/assignee fetch join, 프로젝트 소속 검증 포함
		Task parentTask = taskRepository
				.findParentTaskByPublicIdAndProjectPublicId(parentTaskPublicId, projectPublicId)
				.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		validateCanCreateSubtask(reporterPublicId, reporter, parentTask);

		ProjectMember assigneeMember = getAssigneeMember(
				reporterPublicId, projectPublicId, req.assigneePublicId(), reporter);

		validateAssignPermission(reporter, assigneeMember, req.assigneePublicId());

		Project project = reporter.getProject();
		User reporterUser = reporter.getUser();
		User assigneeUser = assigneeMember.getUser();

		Task subTask = Task.createTask(
				project, req.name(), req.description(),
				assigneeUser, req.priority(), parentTask, reporterUser, req.dueDate()
		);

		taskRepository.save(subTask);
		log.info("하위 업무 생성 - taskName={}, parentTaskId={}, reporterId={}, assigneeId={}",
				req.name(), parentTaskPublicId, reporterPublicId, req.assigneePublicId());

		sendTaskAssignedNotification(reporterPublicId, assigneeUser, subTask, req.name(), projectPublicId);

		return subTask.getPublicId();
	}

	@Transactional(readOnly = true)
	public Page<TaskResponse> getTasks(UUID requesterPublicId, UUID projectPublicId, TaskSearchCond cond, Pageable pageable) {
		validateViewPermission(requesterPublicId, projectPublicId);
		return taskRepository.findTasksByCondition(projectPublicId, cond, pageable)
							 .map(TaskResponse::from);
	}

	@Transactional(readOnly = true)
	public TaskDetailsResponse getTaskDetails(UUID requesterPublicId, UUID projectPublicId, UUID taskPublicId) {
		validateViewPermission(requesterPublicId, projectPublicId);
		Task task = taskRepository.findTaskDetailsByPublicId(taskPublicId)
								  .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));
		return TaskDetailsResponse.from(task);
	}

	/**
	 * 업무 수정 권한
	 * - ADMIN: 모든 필드 수정 가능
	 * - MEMBER(reporter): 모든 필드 수정 가능
	 * - MEMBER(assignee): 상태를 TODO/IN_PROGRESS/IN_REVIEW 범위 내에서만 변경 가능
	 * - VIEWER: 수정 불가
	 * (하위 업무도 동일 로직 적용)
	 */
	public void updateTask(UUID requesterPublicId, UUID projectPublicId, UUID taskPublicId, UpdateTaskRequest req) {
		ProjectMember requester = projectMemberRepository
				.findByUserPublicIdAndProjectPublicId(requesterPublicId, projectPublicId)
				.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		Task task = (req.status() == TaskStatus.CANCELLED)
				? taskRepository.findTaskWithSubTasksAndMembers(taskPublicId)
								.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED))
				: taskRepository.findByPublicIdWithReporterAndAssignee(taskPublicId)
								.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		// ── 알림용 캡처 (변경 전) ──
		TaskStatus previousStatus = task.getStatus();
		User previousAssignee = task.getAssignee();

		boolean isAdmin = requester.getRole() == ProjectRole.ADMIN;
		boolean isReporter = task.getReporter().getPublicId().equals(requesterPublicId);
		boolean isAssignee = task.getAssignee().getPublicId().equals(requesterPublicId);

		if (isAdmin || isReporter) {
			applyFullUpdate(task, req, projectPublicId, requester);
		} else if (isAssignee) {
			applyAssigneeUpdate(task, req);
		} else {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}

		sendTaskUpdateNotifications(requesterPublicId, projectPublicId, task, previousStatus, previousAssignee);

		String role = isAdmin ? "ADMIN" : isReporter ? "REPORTER" : "ASSIGNEE";
		log.info("업무 수정 - taskId={}, projectId={}, requesterId={}, role={}, updatedFields={}",
				taskPublicId, projectPublicId, requesterPublicId, role, req.getUpdatedFields());
	}

	/**
	 * 업무 삭제 권한
	 * - ADMIN : 모든 업무 삭제 가능
	 * - MEMBER: 본인이 reporter인 업무만 삭제 가능 (하위 업무 포함)
	 * - VIEWER: 삭제 불가
	 */
	public void deleteTask(UUID requesterPublicId, UUID projectPublicId, UUID taskPublicId) {
		ProjectMember requester = projectMemberRepository
				.findByUserPublicIdAndProjectPublicId(requesterPublicId, projectPublicId)
				.orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		Task task = taskRepository.findTaskWithSubTasksAndMembers(taskPublicId)
								  .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		boolean isAdmin = requester.getRole() == ProjectRole.ADMIN;
		boolean isReporter = task.getReporter().getPublicId().equals(requesterPublicId);

		if (!isAdmin && !isReporter) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}

		task.softDelete();
		log.info("업무 삭제 - taskId={}, projectId={}, requesterId={}", taskPublicId, projectPublicId, requesterPublicId);
	}

	// 헬퍼 -------------------------

	private ProjectMember getAssigneeMember(UUID reporterPublicId, UUID projectPublicId,
											UUID assigneePublicId, ProjectMember reporter) {
		return reporterPublicId.equals(assigneePublicId)
				? reporter
				: projectMemberRepository.findMemberWithUser(assigneePublicId, projectPublicId)
										 .orElseThrow(() -> new CustomException(PROJECT_MEMBER_NOT_FOUND));
	}

	// ADMIN 또는 부모 업무의 reporter/assignee만 하위 업무 생성 가능
	private void validateCanCreateSubtask(UUID reporterPublicId, ProjectMember reporter, Task parentTask) {
		boolean isAdmin = reporter.getRole() == ProjectRole.ADMIN;
		boolean isParentReporter = parentTask.getReporter().getPublicId().equals(reporterPublicId);
		boolean isParentAssignee = parentTask.getAssignee().getPublicId().equals(reporterPublicId);

		if (!isAdmin && !isParentReporter && !isParentAssignee) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}
	}

	/**
	 * 조회 권한 검증
	 * - 비공개 프로젝트: 프로젝트 멤버만 조회 가능
	 * - 공개 프로젝트 : 워크스페이스 멤버면 조회 가능
	 */
	private void validateViewPermission(UUID requesterPublicId, UUID projectPublicId) {
		Project project = projectRepository.findByPublicIdWithWorkspace(projectPublicId)
										   .orElseThrow(() -> new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED));

		if (project.isPrivate()) {
			if (!projectMemberRepository.existsByProjectPublicIdAndUserPublicId(projectPublicId, requesterPublicId)) {
				throw new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED);
			}
		} else {
			UUID workspacePublicId = project.getWorkspace().getPublicId();
			if (!workspaceMemberRepository.existsByWorkspacePublicIdAndUserPublicId(workspacePublicId, requesterPublicId)) {
				throw new CustomException(TASK_NOT_FOUND_OR_ACCESS_DENIED);
			}
		}
	}

	// ADMIN/REPORTER 전용 — 모든 필드 수정 + CANCELLED 연쇄 처리
	private void applyFullUpdate(Task task, UpdateTaskRequest req, UUID projectPublicId, ProjectMember requester) {
		User newAssignee = resolveNewAssignee(req.assigneePublicId(), projectPublicId, requester);

		if (req.status() == TaskStatus.CANCELLED) {
			task.cancelWithSubTasks();
			task.update(req.name(), req.description(), null, newAssignee, req.priority(), req.dueDate());
		} else {
			task.update(req.name(), req.description(), req.status(), newAssignee, req.priority(), req.dueDate());
		}
	}

	// ASSIGNEE 전용 — 상태만 제한적으로 변경 가능
	private void applyAssigneeUpdate(Task task, UpdateTaskRequest req) {
		if (req.name() != null || req.description() != null || req.priority() != null
				|| req.assigneePublicId() != null || req.dueDate() != null) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}
		if (req.status() != null) {
			if (!ASSIGNEE_ALLOWED_STATUSES.contains(req.status())) {
				throw new CustomException(INSUFFICIENT_PERMISSION);
			}
			task.update(null, null, req.status(), null, null, null);
		}
	}

	private User resolveNewAssignee(UUID assigneePublicId, UUID projectPublicId, ProjectMember requester) {
		if (assigneePublicId == null) return null;

		ProjectMember assigneeMember = projectMemberRepository
				.findMemberWithUser(assigneePublicId, projectPublicId)
				.orElseThrow(() -> new CustomException(PROJECT_MEMBER_NOT_FOUND));

		validateAssignPermission(requester, assigneeMember, assigneePublicId);

		return assigneeMember.getUser();
	}

	/**
	 * 할당 권한 검증 (생성/수정 공통)
	 * - ADMIN : ADMIN/MEMBER에게 할당 가능
	 * - MEMBER: 본인 혹은 다른 MEMBER에게만 할당 가능
	 * - 누구도 VIEWER에게 할당 불가
	 */
	private void validateAssignPermission(ProjectMember requester, ProjectMember assigneeMember, UUID assigneePublicId) {
		ProjectRole assigneeRole = assigneeMember.getRole();

		// 누구도 VIEWER에게 할당 불가
		if (assigneeRole == ProjectRole.VIEWER) {
			throw new CustomException(CANNOT_ASSIGN_TASK_TO_VIEWER);
		}

		// MEMBER는 본인 혹은 다른 MEMBER에게만 할당 가능
		if (requester.getRole() == ProjectRole.MEMBER) {
			boolean isSelf = assigneePublicId.equals(requester.getUser().getPublicId());
			boolean isMember = assigneeRole == ProjectRole.MEMBER;
			if (!isSelf && !isMember) {
				throw new CustomException(INSUFFICIENT_PERMISSION);
			}
		}
	}

	// 알림 헬퍼
	private void publishTaskNotification(Long recipientId, NotificationType type,
										 UUID taskPublicId, UUID projectPublicId, String message) {
		eventPublisher.publishEvent(new NotificationEvent(
				recipientId, type, ReferenceType.TASK, taskPublicId, projectPublicId, message
		));
	}

	private void sendTaskAssignedNotification(UUID reporterPublicId, User assigneeUser,
											  Task task, String taskName, UUID projectPublicId) {
		if (!reporterPublicId.equals(assigneeUser.getPublicId())) {
			publishTaskNotification(
					assigneeUser.getId(), NotificationType.TASK_ASSIGNED, task.getPublicId(), projectPublicId,
					String.format("'%s' 업무가 회원님에게 할당되었습니다.", taskName)
			);
		}
	}

	/**
	 * [업무 수정 알림 발송 로직]
	 * 1. 요청자 제외: 변경을 요청한 당사자(requester)에게는 알림을 보내지 않음.
	 * 2. 중복 방지: 동일 인물이 여러 역할(Reporter, Assignee 등)을 겸할 경우 알림은 한 번만 발송.
	 * 3. 관계자 포괄: ADMIN 권한을 가진 제3자가 개입할 수 있으므로, 영향을 받는 모든 관계자(기존/새 담당자, 보고자)를 고려.
	 * * [1. 상태 변경 알림]
	 *   - 대상: 보고자(Reporter), 현재 담당자(Assignee)
	 *   - 조건:
	 *     - 보고자: 요청자가 아닐 때 발송
	 *     - 담당자: 요청자가 아니고, 보고자와 동일 인물이 아닐 때 발송 (중복 방지)
	 * * [2. 담당자 변경 알림]
	 *   - 대상: 기존 담당자(Previous), 새 담당자(Current), 보고자(Reporter)
	 *   - 조건:
	 *     - 기존 담당자: 요청자가 아닐 때 발송 (누군가에 의해 업무에서 제외됨)
	 *     - 새 담당자: 요청자가 아닐 때 발송 (누군가에 의해 업무가 할당됨 - 셀프 할당 제외)
	 *     - 보고자: 요청자가 아니고, 기존/새 담당자와 동일 인물이 아닐 때 발송 (ADMIN 권한을 가진 제3자가 변경한 경우 대응)
	 */
	private void sendTaskUpdateNotifications(UUID requesterPublicId, UUID projectPublicId, Task task,
											 TaskStatus previousStatus, User previousAssignee) {
		UUID taskPublicId = task.getPublicId();
		User reporter = task.getReporter();
		User currentAssignee = task.getAssignee();

		// 1. 상태 변경 알림
		// 원칙: 변경을 요청한 본인(requester)은 제외, 나머지 관계자에게 발송
		if (previousStatus != task.getStatus()) {
			String statusMessage = String.format("'%s' 업무 상태가 %s(으)로 변경되었습니다.", task.getName(), task.getStatus().getDescription());

			Set<Long> statusTargets = new HashSet<>();

			if (!reporter.getPublicId().equals(requesterPublicId)) {
				statusTargets.add(reporter.getId());
			}

			if (!currentAssignee.getPublicId().equals(requesterPublicId)) {
				statusTargets.add(currentAssignee.getId());
			}

			statusTargets.forEach(id -> publishTaskNotification(id, NotificationType.TASK_STATUS_CHANGED,
					taskPublicId, projectPublicId, statusMessage));
		}

		// 2. 담당자 변경 알림
		// 원칙: 변경을 요청한 본인(requester)은 제외
		// ADMIN 권한을 가진 제3자가 변경할 수 있으므로 reporter에게도 알림
		if (!previousAssignee.getPublicId().equals(currentAssignee.getPublicId())) {
			Map<Long, NotifyAction> assignTargets = new HashMap<>();

			// 1. 기존 assignee에게 (본인이 스스로 내려놓은 게 아니라면)
			if (!previousAssignee.getPublicId().equals(requesterPublicId)) {
				assignTargets.put(previousAssignee.getId(), new NotifyAction(
						NotificationType.TASK_REASSIGNED, String.format("'%s' 업무의 담당자에서 제외되었습니다.", task.getName())
				));
			}

			// 2. 새 assignee에게 (본인이 셀프 할당한 게 아니라면)
			if (!currentAssignee.getPublicId().equals(requesterPublicId)) {
				assignTargets.put(currentAssignee.getId(), new NotifyAction(
						NotificationType.TASK_ASSIGNED, String.format("'%s' 업무가 회원님에게 할당되었습니다.", task.getName())
				));
			}

			// 3. reporter에게 (ADMIN 권한을 가진 제3자가 변경한 경우 reporter도 알아야 함)
			// - 중복 방지: putIfAbsent를 써서 reporter가 기존/새 assignee로서 이미 알림을 받았다면 제외
			if (!reporter.getPublicId().equals(requesterPublicId)) {
				assignTargets.put(reporter.getId(), new NotifyAction(
						NotificationType.TASK_REASSIGNED, String.format("'%s' 업무의 담당자가 변경되었습니다.", task.getName())
				));
			}

			assignTargets.forEach((id, action) -> publishTaskNotification(id, action.type(),
					taskPublicId, projectPublicId, action.message()));
		}
	}
}

record NotifyAction(NotificationType type, String message) {}