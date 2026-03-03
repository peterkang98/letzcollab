package xyz.letzcollab.backend.dto.workspace;

import jakarta.validation.constraints.Size;

public record MemberUpdateMyselfRequest(
	@Size(max = 100, message = "직책은 100자 이내로 입력해주세요.")
	String position
) {
}
