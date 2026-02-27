package xyz.letzcollab.backend.dto.workspace;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceInviteRequest(
	@NotBlank(message = "피초청인의 이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@Size(max = 100, message = "피초청인의 직책은 50자 이내로 입력해주세요.")
	String position
) {
}
