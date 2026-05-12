package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.WorkspaceStatsSnapshot;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceStatsSnapshotRepository extends JpaRepository<WorkspaceStatsSnapshot, Long> {
	Optional<WorkspaceStatsSnapshot> findByWorkspaceId(Long workspaceId);

	Optional<WorkspaceStatsSnapshot> findByWorkspacePublicId(UUID workspacePublicId);

	@Modifying
	@Query(value = """
			INSERT INTO workspace_stats_snapshots (
				workspace_id, workspace_public_id, total_members, total_projects, planned_projects, active_projects,
				on_hold_projects, completed_projects, archived_projects, total_tasks, todo_tasks, in_progress_tasks,
				in_review_tasks, done_tasks, cancelled_tasks, overdue_tasks, created_at, updated_at
			)
			SELECT
				w.workspace_id,
				w.public_id,
				COALESCE(m.total_members, 0),
				COALESCE(p.total_projects, 0),
				COALESCE(p.planned_projects, 0),
				COALESCE(p.active_projects, 0),
				COALESCE(p.on_hold_projects, 0),
				COALESCE(p.completed_projects, 0),
				COALESCE(p.archived_projects, 0),
				COALESCE(t.total_tasks, 0),
				COALESCE(t.todo_tasks, 0),
				COALESCE(t.in_progress_tasks, 0),
				COALESCE(t.in_review_tasks, 0),
				COALESCE(t.done_tasks, 0),
				COALESCE(t.cancelled_tasks, 0),
				COALESCE(t.overdue_tasks, 0),
				NOW(),
				NOW()
			FROM workspaces w
			-- [멤버 수 집계]
			LEFT JOIN (
				SELECT workspace_id, COUNT(*) AS total_members
				FROM workspace_members
				GROUP BY workspace_id
			) m ON w.workspace_id = m.workspace_id
			-- [프로젝트 통계 집계]
			LEFT JOIN (
				SELECT
				   workspace_id,
				   COUNT(project_id) AS total_projects,
				   SUM(CASE WHEN status = 'PLANNED' THEN 1 ELSE 0 END) AS planned_projects,
				   SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_projects,
				   SUM(CASE WHEN status = 'ON_HOLD' THEN 1 ELSE 0 END) AS on_hold_projects,
				   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_projects,
				   SUM(CASE WHEN status = 'ARCHIVED' THEN 1 ELSE 0 END) AS archived_projects
				FROM projects
				GROUP BY workspace_id
			) p ON w.workspace_id = p.workspace_id
			-- [업무 통계 집계]
			LEFT JOIN (
				SELECT
				   p.workspace_id,
				   COUNT(t.task_id) AS total_tasks,
				   SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) AS todo_tasks,
				   SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_tasks,
				   SUM(CASE WHEN t.status = 'IN_REVIEW' THEN 1 ELSE 0 END) AS in_review_tasks,
				   SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS done_tasks,
				   SUM(CASE WHEN t.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_tasks,
				   SUM(CASE WHEN t.due_date < :today AND t.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END) AS overdue_tasks
				FROM tasks t
				JOIN projects p ON t.project_id = p.project_id
				GROUP BY p.workspace_id
			) t ON w.workspace_id = t.workspace_id
			-- [UPSERT]
			ON CONFLICT (workspace_id)
			DO UPDATE SET
				total_members = EXCLUDED.total_members,
				total_projects = EXCLUDED.total_projects,
				planned_projects = EXCLUDED.planned_projects,
				active_projects = EXCLUDED.active_projects,
				on_hold_projects = EXCLUDED.on_hold_projects,
				completed_projects = EXCLUDED.completed_projects,
				archived_projects = EXCLUDED.archived_projects,
				total_tasks = EXCLUDED.total_tasks,
				todo_tasks = EXCLUDED.todo_tasks,
				in_progress_tasks = EXCLUDED.in_progress_tasks,
				in_review_tasks = EXCLUDED.in_review_tasks,
				done_tasks = EXCLUDED.done_tasks,
				cancelled_tasks = EXCLUDED.cancelled_tasks,
				overdue_tasks = EXCLUDED.overdue_tasks,
				updated_at = NOW()
			""",
			nativeQuery = true)
	void updateSnapshots(@Param("today") LocalDate today);


	@Modifying
	@Query(value = """
			INSERT INTO workspace_stats_snapshots (
				workspace_id, workspace_public_id, total_members, total_projects, planned_projects, active_projects,
				on_hold_projects, completed_projects, archived_projects, total_tasks, todo_tasks, in_progress_tasks,
				in_review_tasks, done_tasks, cancelled_tasks, overdue_tasks, created_at, updated_at
			)
			SELECT
				w.workspace_id,
				w.public_id,
				COALESCE(m.total_members, 0),
				COALESCE(p.total_projects, 0),
				COALESCE(p.planned_projects, 0),
				COALESCE(p.active_projects, 0),
				COALESCE(p.on_hold_projects, 0),
				COALESCE(p.completed_projects, 0),
				COALESCE(p.archived_projects, 0),
				COALESCE(t.total_tasks, 0),
				COALESCE(t.todo_tasks, 0),
				COALESCE(t.in_progress_tasks, 0),
				COALESCE(t.in_review_tasks, 0),
				COALESCE(t.done_tasks, 0),
				COALESCE(t.cancelled_tasks, 0),
				COALESCE(t.overdue_tasks, 0),
				NOW(),
				NOW()
			FROM workspaces w
			-- [멤버 수 집계]
			LEFT JOIN (
				SELECT :workspaceId AS ws_id, COUNT(*) AS total_members
				FROM workspace_members
				WHERE workspace_id = :workspaceId
			) m ON w.workspace_id = m.ws_id
			-- [프로젝트 통계 집계]
			LEFT JOIN (
				SELECT
				   :workspaceId AS ws_id,
				   COUNT(project_id) AS total_projects,
				   SUM(CASE WHEN status = 'PLANNED' THEN 1 ELSE 0 END) AS planned_projects,
				   SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_projects,
				   SUM(CASE WHEN status = 'ON_HOLD' THEN 1 ELSE 0 END) AS on_hold_projects,
				   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_projects,
				   SUM(CASE WHEN status = 'ARCHIVED' THEN 1 ELSE 0 END) AS archived_projects
				FROM projects
				WHERE workspace_id = :workspaceId
			) p ON w.workspace_id = p.ws_id
			-- [업무 통계 집계]
			LEFT JOIN (
				SELECT
				   :workspaceId AS ws_id,
				   COUNT(t.task_id) AS total_tasks,
				   SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) AS todo_tasks,
				   SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_tasks,
				   SUM(CASE WHEN t.status = 'IN_REVIEW' THEN 1 ELSE 0 END) AS in_review_tasks,
				   SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS done_tasks,
				   SUM(CASE WHEN t.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_tasks,
				   SUM(CASE WHEN t.due_date < :today AND t.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END) AS overdue_tasks
				FROM tasks t
				JOIN projects p ON t.project_id = p.project_id
				WHERE p.workspace_id = :workspaceId
			) t ON w.workspace_id = t.ws_id
			WHERE w.workspace_id = :workspaceId
			-- [UPSERT]
			ON CONFLICT (workspace_id)
			DO UPDATE SET
				total_members = EXCLUDED.total_members,
				total_projects = EXCLUDED.total_projects,
				planned_projects = EXCLUDED.planned_projects,
				active_projects = EXCLUDED.active_projects,
				on_hold_projects = EXCLUDED.on_hold_projects,
				completed_projects = EXCLUDED.completed_projects,
				archived_projects = EXCLUDED.archived_projects,
				total_tasks = EXCLUDED.total_tasks,
				todo_tasks = EXCLUDED.todo_tasks,
				in_progress_tasks = EXCLUDED.in_progress_tasks,
				in_review_tasks = EXCLUDED.in_review_tasks,
				done_tasks = EXCLUDED.done_tasks,
				cancelled_tasks = EXCLUDED.cancelled_tasks,
				overdue_tasks = EXCLUDED.overdue_tasks,
				updated_at = NOW()
			""",
			nativeQuery = true)
	void updateSnapshotByWorkspaceId(
			@Param("workspaceId") Long workspaceId,
			@Param("today") LocalDate today
	);
}
