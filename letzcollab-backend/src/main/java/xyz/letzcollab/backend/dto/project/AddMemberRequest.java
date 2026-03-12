package xyz.letzcollab.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

import java.util.UUID;

@Schema(description = "프로젝트 멤버 추가 요청 DTO")
public record AddMemberRequest(

		@Schema(description = "초대할 대상의 User Public ID", example = "123e4567-e89b-12d3-a456-426614174000")
		@NotNull(message = "초대할 사용자를 선택해주세요.")
		UUID targetUserPublicId,

		@Schema(description = "부여할 프로젝트 권한", example = "MEMBER")
		@NotNull(message = "부여할 프로젝트 권한을 선택해주세요.")
		ProjectRole role,

		@Schema(description = "부여할 역할(직책)", example = "백엔드 개발", nullable = true)
		@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요. (예: 백엔드 개발, UI 디자인)")
		String position
) {
}
