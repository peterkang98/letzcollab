package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.dto.project.ProjectRawStatsDto;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.Workspace;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {

	@Query("SELECT p " +
			"FROM Project p " +
			"JOIN FETCH p.leader " +
			"WHERE p.publicId = :projectPublicId")
	Optional<Project> findByPublicIdWithLeader(@Param("projectPublicId") UUID projectPublicId);

	boolean existsByWorkspaceAndName(Workspace workspace, String name);

	// 업무 조회 권한 검증용
	@Query("SELECT p " +
			"FROM Project p " +
			"JOIN FETCH p.workspace " +
			"WHERE p.publicId = :projectPublicId")
	Optional<Project> findByPublicIdWithWorkspace(@Param("projectPublicId") UUID projectPublicId);

	// 특정 워크스페이스의 프로젝트 통계를 실시간으로 집계
	@Query("""
			SELECT new xyz.letzcollab.backend.dto.project.ProjectRawStatsDto(
				COUNT(DISTINCT p.id),
				COALESCE(SUM(CASE WHEN p.status = 'PLANNED' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN p.status = 'ACTIVE' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN p.status = 'ON_HOLD' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END), 0),
				COALESCE(SUM(CASE WHEN p.status = 'ARCHIVED' THEN 1 ELSE 0 END), 0)
			)
			FROM Project p
			WHERE p.workspace.publicId = :workspacePublicId
			""")
	ProjectRawStatsDto aggregateProjectStats(@Param("workspacePublicId") UUID workspacePublicId, @Param("today") LocalDate today);
}
