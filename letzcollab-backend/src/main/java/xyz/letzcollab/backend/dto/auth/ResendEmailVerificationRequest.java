package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "이메일 인증 재발송 요청 DTO")
public record ResendEmailVerificationRequest(
	@Schema(description = "만료된 기존 이메일 인증 토큰", example = "123e4567-e89b-12d3-a456-426614174000")
	@NotNull(message = "인증 토큰은 필수입니다.")
	UUID expiredToken
) {
}
