package xyz.letzcollab.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record PasswordResetRequest(
	@NotBlank(message = "검증 토큰은 필수입니다.")
	UUID token,

	@NotBlank(message = "비밀번호는 필수입니다")
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
			message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."
	)
	String newPassword
) {
}
