package xyz.letzcollab.backend.dto.comment;

import xyz.letzcollab.backend.entity.TaskComment;

import java.time.LocalDateTime;

public record ReplyResponse(
		Long commentId,
		String authorName,
		String content,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static ReplyResponse from(TaskComment comment) {
		return new ReplyResponse(
				comment.getId(),
				comment.getAuthor().getName(),
				comment.getContent(),
				comment.getCreatedAt(),
				comment.getUpdatedAt()
		);
	}
}