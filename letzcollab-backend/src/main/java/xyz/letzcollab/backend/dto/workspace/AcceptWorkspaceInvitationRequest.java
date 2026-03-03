package xyz.letzcollab.backend.dto.workspace;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptWorkspaceInvitationRequest(
	@NotNull(message = "초대 토큰은 필수입니다.")
	UUID token
) {
}
