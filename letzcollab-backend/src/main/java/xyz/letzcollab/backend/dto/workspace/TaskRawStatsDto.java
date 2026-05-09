package xyz.letzcollab.backend.dto.workspace;

public record TaskRawStatsDto(
	long totalTasks,
	long todoTasks,
	long inProgressTasks,
	long inReviewTasks,
	long doneTasks,
	long cancelledTasks,
	long overdueTasks
) {
}
