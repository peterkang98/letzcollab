package xyz.letzcollab.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResendEmailVerificationRequest(
	@NotBlank(message = "검증 토큰은 필수입니다.")
	@Pattern(
		regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
		message = "유효하지 않은 토큰 형식입니다."
	)
	String expiredToken
) {
}
