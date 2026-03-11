package xyz.letzcollab.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;
import xyz.letzcollab.backend.repository.NotificationRepository;
import xyz.letzcollab.backend.repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDeadlineScheduler {

	private final TaskRepository taskRepository;
	private final NotificationRepository notificationRepository;

	/**
	 * 매일 새벽 3시에 실행
	 * - 마감일이 내일인 미완료 업무 → TASK_DUE_SOON 알림
	 * - 마감일이 어제인 미완료 업무 → TASK_OVERDUE 알림
	 *
	 * 중복 방지: 같은 업무에 대해 같은 type(TASK_DUE_SOON 등)의 알림이 이미 존재하면 스킵
	 */
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void checkTaskDeadlines() {
		LocalDate today = LocalDate.now();
		LocalDate tomorrow = today.plusDays(1);
		LocalDate yesterday = today.minusDays(1);

		// D-1: 마감 임박 알림 (담당자에게만)
		List<Task> dueSoonTasks = taskRepository.findActiveTasksByDueDate(tomorrow);
		for (Task task : dueSoonTasks) {
			createNotificationIfAbsent(
					task.getAssignee(), task.getPublicId(), task.getProject().getPublicId(),
					NotificationType.TASK_DUE_SOON,
					String.format("'%s' 업무의 마감일이 내일입니다.", task.getName())
			);
		}

		// D+1: 마감 초과 알림
		List<Task> overdueTasks = taskRepository.findActiveTasksByDueDate(yesterday);
		for (Task task : overdueTasks) {
			// 담당자에게 알림
			createNotificationIfAbsent(
					task.getAssignee(), task.getPublicId(), task.getProject().getPublicId(),
					NotificationType.TASK_OVERDUE,
					String.format("'%s' 업무가 마감일을 초과했습니다.", task.getName())
			);

			// 보고자가 담당자와 다르면 보고자에게도 알림
			if (!task.getReporter().getId().equals(task.getAssignee().getId())) {
				createNotificationIfAbsent(
						task.getReporter(), task.getPublicId(), task.getProject().getPublicId(),
						NotificationType.TASK_OVERDUE,
						String.format("'%s' 업무가 마감일을 초과했습니다.", task.getName())
				);
			}
		}

		log.info("마감 알림 스케줄러 실행 완료 - dueSoon={}, overdue={}", dueSoonTasks.size(), overdueTasks.size());
	}

	private void createNotificationIfAbsent(User recipient, UUID taskPublicId, UUID projectPublicId,
											NotificationType type, String message) {
		boolean alreadySent = notificationRepository.existsByRecipientIdAndReferenceIdAndType(
				recipient.getId(), taskPublicId, type
		);

		if (!alreadySent) {
			notificationRepository.save(Notification.create(
					recipient, type, ReferenceType.TASK, taskPublicId, projectPublicId, message
			));
		}
	}
}