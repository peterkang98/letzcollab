package xyz.letzcollab.backend.dto.task;

import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.util.UUID;

public record TaskSearchCond(
		TaskStatus status,
		TaskPriority priority,
		UUID assigneePublicId,
		LocalDate dueDateFrom,
		LocalDate dueDateTo
) {
}
