package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "비밀번호 초기화(변경) 수행 요청 DTO")
public record PasswordResetRequest(
	@Schema(description = "이메일로 발송된 비밀번호 초기화 토큰", example = "123e4567-e89b-12d3-a456-426614174000")
	@NotNull(message = "검증 토큰은 필수입니다.")
	UUID token,

	@Schema(description = "새로운 비밀번호 (영문, 숫자, 특수문자 포함 8자 이상)", example = "NewPassword123!")
	@NotBlank(message = "비밀번호는 필수입니다")
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
			message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."
	)
	String newPassword
) {
}
