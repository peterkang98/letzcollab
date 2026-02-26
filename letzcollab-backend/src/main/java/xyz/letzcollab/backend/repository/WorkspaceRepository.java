package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.Workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
	Optional<Workspace> findByPublicId(UUID publicId);

	boolean existsByName(String name);

	@Query("SELECT w " +
			"FROM Workspace w " +
			"JOIN FETCH w.members m " +
			"JOIN FETCH w.owner " +
			"JOIN FETCH m.user " +
			"WHERE w.publicId = :workspacePublicId")
	Optional<Workspace> findWorkspaceWithAllMembers(@Param("workspacePublicId") UUID workspacePublicId);

	@Query("SELECT w " +
			"FROM Workspace w " +
			"JOIN FETCH w.owner " +
			"WHERE w.publicId = :workspacePublicId")
	Optional<Workspace> findWorkspaceByPublicIdWithOwner(@Param("workspacePublicId") UUID workspacePublicId);
}
