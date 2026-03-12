package xyz.letzcollab.backend.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "워크스페이스 생성 요청 DTO")
public record CreateWorkspaceRequest(
		@Schema(description = "워크스페이스 이름", example = "LetzCollab 개발팀")
		@NotBlank(message = "워크스페이스 이름은 필수입니다.")
		@Size(min = 2, max = 30, message = "워크스페이스 이름은 2자 이상 50자 이하로 입력해주세요.")
		String name,

		@Schema(description = "나의 초기 직책", example = "CTO", nullable = true)
		@Size(max = 100, message = "직책은 100자 이내로 입력해주세요.")
		String position
) {
}
