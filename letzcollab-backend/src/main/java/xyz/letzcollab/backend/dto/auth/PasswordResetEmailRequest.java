package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 초기화 이메일 요청 DTO")
public record PasswordResetEmailRequest(
	@Schema(description = "가입된 사용자 이메일", example = "user@letzcollab.xyz")
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email
) {
}
