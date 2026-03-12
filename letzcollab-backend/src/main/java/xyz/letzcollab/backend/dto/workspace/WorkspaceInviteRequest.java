package xyz.letzcollab.backend.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "워크스페이스 멤버 초대 요청 DTO")
public record WorkspaceInviteRequest(
	@Schema(description = "피초청인 이메일", example = "newmember@example.com")
	@NotBlank(message = "피초청인의 이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email,

	@Schema(description = "피초청인에게 부여할 직책", example = "프론트엔드 개발자", nullable = true)
	@Size(max = 100, message = "피초청인의 직책은 50자 이내로 입력해주세요.")
	String position
) {
}
