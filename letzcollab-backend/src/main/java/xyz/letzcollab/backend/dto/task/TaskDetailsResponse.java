package xyz.letzcollab.backend.dto.task;

import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TaskDetailsResponse(
		UUID publicId,
		String name,
		String description,
		TaskStatus status,
		TaskPriority priority,
		String assigneeName,
		UUID assigneePublicId,
		String reporterName,
		UUID reporterPublicId,
		UUID parentTaskPublicId,
		List<TaskResponse> subTasks,
		LocalDate dueDate,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static TaskDetailsResponse from(Task task) {
		return new TaskDetailsResponse(
				task.getPublicId(),
				task.getName(),
				task.getDescription(),
				task.getStatus(),
				task.getPriority(),
				task.getAssignee().getName(),
				task.getAssignee().getPublicId(),
				task.getReporter().getName(),
				task.getReporter().getPublicId(),
				task.getParentTask() != null ? task.getParentTask().getPublicId() : null,
				task.getSubTasks().stream().map(TaskResponse::from).toList(),
				task.getDueDate(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}
}