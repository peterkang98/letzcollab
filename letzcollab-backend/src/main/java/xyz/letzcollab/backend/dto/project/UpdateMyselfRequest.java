package xyz.letzcollab.backend.dto.project;

import jakarta.validation.constraints.Size;

public record UpdateMyselfRequest(
		@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요.")
		String newPosition
) {
}
