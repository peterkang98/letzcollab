package xyz.letzcollab.backend.dto.workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.letzcollab.backend.entity.WorkspaceStatsSnapshot;

import java.time.LocalDateTime;

@Schema(description = "워크스페이스 통계 응답 DTO")
public record WorkspaceStatsResponse(
	@Schema(description = "프로젝트 통계")
	ProjectStats projects,

	@Schema(description = "업무 통계")
	TaskStats tasks,

	@Schema(description = "전체 멤버 수")
	long totalMembers,

	@Schema(description = "갱신 시간")
	LocalDateTime updatedAt
) {

	@Schema(description = "프로젝트 통계")
	public record ProjectStats(
		@Schema(description = "전체 프로젝트 수") long total,
		@Schema(description = "기획된 프로젝트 수") long planned,
		@Schema(description = "진행 중인 프로젝트 수") long active,
		@Schema(description = "일시 중단된 프로젝트 수") long onHold,
		@Schema(description = "완료된 프로젝트 수") long completed,
		@Schema(description = "보관된 프로젝트 수") long archived
	) {}

	@Schema(description = "업무 통계")
	public record TaskStats(
		@Schema(description = "전체 업무 수") long total,
		@Schema(description = "시작 전 업무 수") long todo,
		@Schema(description = "진행 중 업무 수") long inProgress,
		@Schema(description = "검토 중 업무 수") long inReview,
		@Schema(description = "완료된 업무 수") long done,
		@Schema(description = "취소된 업무 수") long cancelled,
		@Schema(description = "마감 초과 업무 수") long overdue,
		@Schema(description = "업무 완료율 (0~100)", example = "72") int completionRate
	) {}

	public static WorkspaceStatsResponse from(WorkspaceStatsSnapshot snapshot) {
		int completionRate = getCompletionRate(
				snapshot.getTotalTasks(), snapshot.getCancelledTasks(), snapshot.getDoneTasks()
		);

		return new WorkspaceStatsResponse(
			new ProjectStats(
					snapshot.getTotalProjects(),
					snapshot.getPlannedProjects(),
					snapshot.getActiveProjects(),
					snapshot.getOnHoldProjects(),
					snapshot.getCompletedProjects(),
					snapshot.getArchivedProjects()
			),
			new TaskStats(
					snapshot.getTotalTasks(),
					snapshot.getTodoTasks(),
					snapshot.getInProgressTasks(),
					snapshot.getInReviewTasks(),
					snapshot.getDoneTasks(),
					snapshot.getCancelledTasks(),
					snapshot.getOverdueTasks(),
					completionRate
			),
			snapshot.getTotalMembers(),
			snapshot.getUpdatedAt()
		);
	}

	private static int getCompletionRate(long totalTasks, long cancelledTasks, long doneTasks) {
		int completionRate = 0;
		long effective = totalTasks - cancelledTasks;

		if (effective > 0) {
			completionRate = (int) Math.round(doneTasks * 100.0 / effective);
		}
		return completionRate;
	}
}
