package xyz.letzcollab.backend.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "워크스페이스 초대 수락 DTO")
public record AcceptWorkspaceInvitationRequest(
	@Schema(description = "초대 수락 토큰(UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
	@NotNull(message = "초대 토큰은 필수입니다.")
	UUID token
) {
}
