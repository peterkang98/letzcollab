package xyz.letzcollab.backend.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 생성 요청 DTO")
public record CreateCommentRequest(
		@Schema(description = "댓글 내용", example = "이 쿼리는 인덱스를 태우는 게 좋겠습니다.")
		@NotBlank(message = "댓글 내용은 필수입니다.")
		@Size(max = 10000, message = "댓글은 최대 10,000자까지 가능합니다.")
		String content,

		@Schema(description = "부모 댓글 ID (null 이면 최상위 댓글, 값이 있으면 대댓글)", example = "1", nullable = true)
		Long parentCommentId  // null이면 최상위 댓글, 값이 있으면 대댓글
) {
}