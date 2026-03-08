package xyz.letzcollab.backend.dto.project;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

import java.util.UUID;

public record AddMemberRequest(
		@NotNull(message = "초대할 사용자를 선택해주세요.")
		UUID targetUserPublicId,

		@NotNull(message = "부여할 프로젝트 권한을 선택해주세요.")
		ProjectRole role,

		@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요. (예: 백엔드 개발, UI 디자인)")
		String position
) {
}
