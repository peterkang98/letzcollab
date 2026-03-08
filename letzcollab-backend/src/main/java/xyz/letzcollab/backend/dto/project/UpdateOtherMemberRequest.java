package xyz.letzcollab.backend.dto.project;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

import java.util.UUID;

public record UpdateOtherMemberRequest(
		@NotNull(message = "수정할 사용자를 선택해주세요.")
		UUID targetUserPublicId,

		@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요.")
		String newPosition,

		ProjectRole newRole
) {
}
