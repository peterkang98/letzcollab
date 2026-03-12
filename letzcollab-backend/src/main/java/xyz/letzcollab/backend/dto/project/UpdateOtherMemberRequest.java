package xyz.letzcollab.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

import java.util.UUID;

@Schema(description = "타 프로젝트 멤버 정보 수정 요청 DTO")
public record UpdateOtherMemberRequest(
		@Schema(description = "수정할 대상의 User Public ID")
		@NotNull(message = "수정할 사용자를 선택해주세요.")
		UUID targetUserPublicId,

		@Schema(description = "변경할 역할(직책)", example = "데이터베이스 설계", nullable = true)
		@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요.")
		String newPosition,

		@Schema(description = "변경할 권한 수준", example = "ADMIN", nullable = true)
		ProjectRole newRole
) {
}
