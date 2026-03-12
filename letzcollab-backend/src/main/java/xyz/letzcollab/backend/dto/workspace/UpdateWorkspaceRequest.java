package xyz.letzcollab.backend.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "워크스페이스 정보 수정 요청 DTO")
public record UpdateWorkspaceRequest(
	@Schema(description = "새로운 워크스페이스 이름", example = "LetzCollab 신규 개발팀")
	@NotBlank(message = "워크스페이스 이름은 필수입니다.")
	@Size(min = 2, max = 50, message = "워크스페이스 이름은 2자 이상 50자 이하로 입력해주세요.")
	String newName
) {
}
