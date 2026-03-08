package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.Workspace;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {

	@Query("SELECT p " +
			"FROM Project p " +
			"JOIN FETCH p.leader " +
			"WHERE p.publicId = :projectPublicId")
	Optional<Project> findByPublicIdWithLeader(@Param("projectPublicId") UUID projectPublicId);

	boolean existsByWorkspaceAndName(Workspace workspace, String name);
}
