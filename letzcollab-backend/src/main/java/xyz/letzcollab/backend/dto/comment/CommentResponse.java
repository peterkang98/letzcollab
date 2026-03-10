package xyz.letzcollab.backend.dto.comment;

import xyz.letzcollab.backend.entity.TaskComment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
		Long commentId,
		String authorName,
		String content,
		List<ReplyResponse> replies,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static CommentResponse from(TaskComment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getAuthor().getName(),
				comment.getContent(),
				comment.getChildComments().stream().map(ReplyResponse::from).toList(),
				comment.getCreatedAt(),
				comment.getUpdatedAt()
		);
	}
}
