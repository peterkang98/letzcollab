package xyz.letzcollab.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 정보 DTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
	@Schema(description = "발급된 JWT 액세스 토큰 (모바일용. 웹일 경우 null)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	String accessToken,

	@Schema(description = "사용자 이름", example = "홍길동")
	String name,

	@Schema(description = "사용자 이메일", example = "user@letzcollab.xyz")
	String email
) {
	public LoginResponse withoutToken() {
		return new LoginResponse(null, this.name, this.email);
	}
}