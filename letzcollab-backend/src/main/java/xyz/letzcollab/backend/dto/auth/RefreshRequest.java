package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 재발급 요청 DTO")
public record RefreshRequest(
	@Schema(description = "갱신 토큰", example = "tVToUCQ93PR_CAUx4lJxhq7r...")
	String refreshToken
) {
}
