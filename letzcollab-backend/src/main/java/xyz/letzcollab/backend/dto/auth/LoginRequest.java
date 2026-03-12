package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequest(
	@Schema(description = "이메일 (로그인 ID)", example = "user@letzcollab.xyz")
	@NotBlank(message = "이메일을 입력하세요")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@Schema(description = "비밀번호", example = "Password123!")
	@NotBlank(message = "비밀번호를 입력하세요")
	String password
) {}
