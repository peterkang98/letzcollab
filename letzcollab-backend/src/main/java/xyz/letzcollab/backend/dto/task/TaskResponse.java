package xyz.letzcollab.backend.dto.task;

import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
		UUID publicId,
		String name,
		TaskStatus status,
		TaskPriority priority,
		String assigneeName,
		UUID assigneePublicId,
		String reporterName,
		UUID parentTaskPublicId,
		LocalDate dueDate,
		LocalDateTime createdAt
) {
	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getPublicId(),
				task.getName(),
				task.getStatus(),
				task.getPriority(),
				task.getAssignee().getName(),
				task.getAssignee().getPublicId(),
				task.getReporter().getName(),
				task.getParentTask() != null ? task.getParentTask().getPublicId() : null,
				task.getDueDate(),
				task.getCreatedAt()
		);
	}
}
