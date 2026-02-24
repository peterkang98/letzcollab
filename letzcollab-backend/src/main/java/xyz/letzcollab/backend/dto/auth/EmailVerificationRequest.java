package xyz.letzcollab.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record EmailVerificationRequest(
	@NotBlank(message = "검증 토큰은 필수입니다.")
	UUID token
) {
}
