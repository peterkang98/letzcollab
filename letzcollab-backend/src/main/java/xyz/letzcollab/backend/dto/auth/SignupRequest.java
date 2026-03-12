package xyz.letzcollab.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 DTO")
public record SignupRequest(
	@Schema(description = "사용자 이름", example = "홍길동")
	@NotBlank(message = "이름은 필수입니다.")
	@Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하로 입력해주세요.")
	String name,

	@Schema(description = "이메일 (로그인 ID)", example = "user@letzcollab.xyz")
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@Schema(description = "비밀번호 (영문, 숫자, 특수문자 포함 8자 이상)", example = "Password123!")
	@NotBlank(message = "비밀번호는 필수입니다")
	@Pattern(
			regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
			message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."
	)
	String password,

	@Schema(description = "전화번호 (선택사항)", example = "010-1234-5678", nullable = true)
	@Pattern(
			regexp = "^(\\d{2,3}-\\d{3,4}-\\d{4})?$",
			message = "올바른 전화번호 형식이거나 빈 값이어야 합니다."
	)
	String phoneNumber
) {}
