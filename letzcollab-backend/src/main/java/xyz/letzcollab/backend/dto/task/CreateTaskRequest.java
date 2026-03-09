package xyz.letzcollab.backend.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.TaskPriority;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
		@NotBlank(message = "업무 이름은 필수입니다.")
		@Size(min = 2, max = 255, message = "업무 이름은 2자 이상 255자 이하로 입력해주세요.")
		String name,

		@Size(max = 10000, message = "설명은 최대 10,000자까지 가능합니다.")
		String description,

		@NotNull(message = "담당자를 선택해주세요.")
		UUID assigneePublicId,

		@NotNull(message = "우선순위를 선택해주세요.")
		TaskPriority priority,

		LocalDate dueDate
) {
}
