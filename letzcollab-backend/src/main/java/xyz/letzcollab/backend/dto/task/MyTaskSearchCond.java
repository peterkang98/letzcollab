package xyz.letzcollab.backend.dto.task;

import jakarta.validation.constraints.NotNull;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MyTaskSearchCond(
		@NotNull(message = "워크스페이스를 선택해주세요.")
		UUID workspacePublicId,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDateFrom,
		LocalDate dueDateTo
) {
}
