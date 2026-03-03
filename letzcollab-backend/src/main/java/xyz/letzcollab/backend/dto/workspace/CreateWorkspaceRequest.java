package xyz.letzcollab.backend.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
		@NotBlank(message = "워크스페이스 이름은 필수입니다.")
		@Size(min = 2, max = 30, message = "워크스페이스 이름은 2자 이상 50자 이하로 입력해주세요.")
		String name,

		@Size(max = 100, message = "직책은 100자 이내로 입력해주세요.")
		String position
) {
}
