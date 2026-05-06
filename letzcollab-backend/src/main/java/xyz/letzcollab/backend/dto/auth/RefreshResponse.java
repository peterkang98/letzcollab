package xyz.letzcollab.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답 DTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefreshResponse(
	@Schema(description = "새로 발급된 JWT 액세스 토큰 (모바일용. 웹일 경우 null)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	String accessToken,

	@Schema(description = "새로 발급된 갱신 토큰 (모바일용. 웹일 경우 null)", example = "tVToUCQ93PR_CAUx4lJxhq7r...")
	String refreshToken
) {
	public RefreshResponse withoutToken() {
		return new RefreshResponse(null, null);
	}
}
