package xyz.letzcollab.backend.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;

@Schema(description = "타 멤버 권한/직책 수정 요청 DTO")
public record MemberUpdateOtherRequest(
		@Schema(description = "변경할 직책", example = "디자이너", nullable = true)
		@Size(max = 100, message = "직책은 100자 이내로 입력해주세요.")
		String position,

		@Schema(description = "변경할 권한 수준 (ADMIN, MEMBER, GUEST)", example = "ADMIN")
		WorkspaceRole role
) {
}
