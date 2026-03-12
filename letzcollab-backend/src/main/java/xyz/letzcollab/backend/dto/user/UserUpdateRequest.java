package xyz.letzcollab.backend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		@Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하로 입력해주세요.")
		String name,

		@Pattern(
				regexp = "^(\\d{2,3}-\\d{3,4}-\\d{4})?$",
				message = "올바른 전화번호 형식이거나 빈 값이어야 합니다."
		)
		String phoneNumber
) {
}
