package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그아웃 요청 DTO")
public record LogoutRequest(
	@Schema(description = "유효한 갱신 토큰", example = "tVToUCQ93PR_CAUx4lJxhq7r...")
	String refreshToken
) {
}
