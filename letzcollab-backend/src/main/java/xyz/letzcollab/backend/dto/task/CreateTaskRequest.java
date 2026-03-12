package xyz.letzcollab.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.TaskPriority;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
		@Schema(description = "업무 이름", example = "N+1 쿼리 문제 해결")
		@NotBlank(message = "업무 이름은 필수입니다.")
		@Size(min = 2, max = 255, message = "업무 이름은 2자 이상 255자 이하로 입력해주세요.")
		String name,

		@Schema(description = "업무 상세 설명", example = "Fetch Join을 적용하여 쿼리 성능을 개선합니다.", nullable = true)
		@Size(max = 10000, message = "설명은 최대 10,000자까지 가능합니다.")
		String description,

		@Schema(description = "담당자 Public ID", example = "123e4567-e89b-12d3-a456-426614174000")
		@NotNull(message = "담당자를 선택해주세요.")
		UUID assigneePublicId,

		@Schema(description = "우선순위 (LOW, MEDIUM, HIGH, URGENT)", example = "HIGH")
		@NotNull(message = "우선순위를 선택해주세요.")
		TaskPriority priority,

		@Schema(description = "마감일", example = "2024-04-15", nullable = true)
		LocalDate dueDate
) {
}
