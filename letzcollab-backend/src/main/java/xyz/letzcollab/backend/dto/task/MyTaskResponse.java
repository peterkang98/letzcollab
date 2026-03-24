package xyz.letzcollab.backend.dto.task;

import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MyTaskResponse(
		UUID publicId,
		String name,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDate,
		String projectName,
		UUID projectPublicId
) {
	public static MyTaskResponse from(Task task) {
		return new MyTaskResponse(
				task.getPublicId(),
				task.getName(),
				task.getStatus(),
				task.getPriority(),
				task.getDueDate(),
				task.getProject().getName(),
				task.getProject().getPublicId()
		);
	}
}