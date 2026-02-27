package xyz.letzcollab.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmailVerificationRequest(
	@NotNull(message = "검증 토큰은 필수입니다.")
	UUID token
) {
}
