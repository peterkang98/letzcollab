package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.dto.workspace.TaskRawStatsDto;
import xyz.letzcollab.backend.entity.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Long>, TaskRepositoryCustom {

	// 수정/삭제 - subtask, reporter, assignee fetch join
	@Query("SELECT t FROM Task t " +
			"LEFT JOIN FETCH t.subTasks " +
			"JOIN FETCH t.reporter " +
			"JOIN FETCH t.assignee " +
			"WHERE t.publicId = :taskPublicId")
	Optional<Task> findTaskWithSubTasksAndMembers(@Param("taskPublicId") UUID taskPublicId);

	// 수정 권한 검증용 — reporter, assignee fetch join
	@Query("SELECT t FROM Task t " +
			"JOIN FETCH t.reporter " +
			"JOIN FETCH t.assignee " +
			"WHERE t.publicId = :publicId")
	Optional<Task> findByPublicIdWithReporterAndAssignee(@Param("publicId") UUID publicId);

	// 하위 업무 생성용 — reporter, assignee fetch join + 프로젝트 소속 검증
	@Query("SELECT t FROM Task t " +
			"JOIN FETCH t.reporter " +
			"JOIN FETCH t.assignee " +
			"JOIN t.project p " +
			"WHERE t.publicId = :taskPublicId AND p.publicId = :projectPublicId")
	Optional<Task> findParentTaskByPublicIdAndProjectPublicId(
			@Param("taskPublicId") UUID taskPublicId,
			@Param("projectPublicId") UUID projectPublicId
	);

	// 댓글 조회용
	Optional<Task> findByPublicIdAndProjectPublicId(UUID publicId, UUID projectPublicId);

	// 댓글 생성 및 알림 생성용 — reporter, assignee fetch join
	@Query("SELECT t FROM Task t " +
			"JOIN FETCH t.reporter " +
			"JOIN FETCH t.assignee " +
			"WHERE t.publicId = :taskPublicId AND t.project.publicId = :projectPublicId")
	Optional<Task> findTaskWithMembersByPublicIds(
			@Param("taskPublicId") UUID taskPublicId,
			@Param("projectPublicId") UUID projectPublicId
	);

	// 스케줄러용: 특정 마감일의 미완료 업무 조회 (assignee, reporter, project fetch join)
	@Query("SELECT t FROM Task t " +
			"JOIN FETCH t.assignee " +
			"JOIN FETCH t.reporter " +
			"JOIN FETCH t.project " +
			"WHERE t.dueDate = :dueDate " +
			"AND t.status NOT IN ('DONE', 'CANCELLED')")
	List<Task> findActiveTasksByDueDate(@Param("dueDate") LocalDate dueDate);

	// 특정 워크스페이스의 업무 통계를 실시간으로 집계
	@Query("""
			SELECT new xyz.letzcollab.backend.dto.workspace.TaskRawStatsDto(
				COUNT(DISTINCT t.id),
				COALESCE(SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN t.status = 'IN_REVIEW' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN t.status = 'CANCELLED' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN t.dueDate < :today AND t.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END), 0)
			)
			FROM Task t
			JOIN Project p on t.project = p
			WHERE p.workspace.publicId = :workspacePublicId
			""")
	TaskRawStatsDto aggregateTaskStats(@Param("workspacePublicId") UUID workspacePublicId, @Param("today") LocalDate today);
}
