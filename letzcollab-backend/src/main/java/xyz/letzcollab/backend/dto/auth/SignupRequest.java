package xyz.letzcollab.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	@NotBlank(message = "이름은 필수입니다.")
	@Size(min = 2, max = 100)
	String name,

	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다")
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
			message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."
	)
	String password,

	@Pattern(
			regexp = "^(\\d{2,3}-\\d{3,4}-\\d{4})?$",
			message = "올바른 전화번호 형식이거나 빈 값이어야 합니다."
	)
	String phoneNumber
) {}
