package xyz.letzcollab.backend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
		@NotBlank(message = "댓글 내용은 필수입니다.")
		@Size(max = 10000, message = "댓글은 최대 10,000자까지 가능합니다.")
		String content,

		Long parentCommentId  // null이면 최상위 댓글, 값이 있으면 대댓글
) {
}