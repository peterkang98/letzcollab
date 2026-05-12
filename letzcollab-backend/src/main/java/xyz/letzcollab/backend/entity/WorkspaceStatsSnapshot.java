package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.letzcollab.backend.global.entity.DateBaseEntity;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "workspace_stats_snapshots")
public class WorkspaceStatsSnapshot extends DateBaseEntity {
	// Workspace와 1:1 매핑이지만, 조회 성능 상 연관관계는 안 맺음
	@Id
	@Column(name = "workspace_id")
	private Long workspaceId;

	@Column(name = "workspace_public_id", columnDefinition = "uuid", nullable = false, updatable = false, unique = true)
	private UUID workspacePublicId;

	@Column(nullable = false, name = "total_members")
	private long totalMembers;

	// 프로젝트 통계
	@Column(nullable = false, name = "total_projects")
	private long totalProjects;

	@Column(nullable = false, name = "planned_projects")
	private long plannedProjects;

	@Column(nullable = false, name = "active_projects")
	private long activeProjects;

	@Column(nullable = false, name = "on_hold_projects")
	private long onHoldProjects;

	@Column(nullable = false, name = "completed_projects")
	private long completedProjects;

	@Column(nullable = false, name = "archived_projects")
	private long archivedProjects;

	// 업무 통계
	@Column(nullable = false, name = "total_tasks")
	private long totalTasks;

	@Column(nullable = false, name = "todo_tasks")
	private long todoTasks;

	@Column(nullable = false, name = "in_progress_tasks")
	private long inProgressTasks;

	@Column(nullable = false, name = "in_review_tasks")
	private long inReviewTasks;

	@Column(nullable = false, name = "done_tasks")
	private long doneTasks;

	@Column(nullable = false, name = "cancelled_tasks")
	private long cancelledTasks;

	@Column(nullable = false, name = "overdue_tasks")
	private long overdueTasks;
}
