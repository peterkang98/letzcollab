package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
	@Query("SELECT wm " +
			"from WorkspaceMember wm " +
			"JOIN FETCH wm.workspace w " +
			"JOIN FETCH w.owner " +
			"WHERE wm.user.publicId = :userPublicId")
	List<WorkspaceMember> findAllWithWorkspace(@Param("userPublicId") UUID userPublicId);


	@Query("SELECT wm " +
			"from WorkspaceMember wm " +
			"JOIN FETCH wm.workspace w " +
			"JOIN FETCH wm.user u " +
			"WHERE w.publicId = :workspacePublicId AND u.publicId = :userPublicId")
	Optional<WorkspaceMember> findMemberWithWorkspaceAndUser(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("userPublicId") UUID userPublicId
	);

	@Query("SELECT wm " +
			"from WorkspaceMember wm " +
			"JOIN FETCH wm.workspace w " +
			"JOIN FETCH wm.user u " +
			"JOIN FETCH w.owner " +
			"WHERE w.publicId = :workspacePublicId AND u.publicId = :userPublicId")
	Optional<WorkspaceMember> findMemberWithWorkspaceAndUserAndOwner(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("userPublicId") UUID userPublicId
	);

	@Query("SELECT wm " +
			"from WorkspaceMember wm " +
			"JOIN wm.workspace w " +
			"JOIN wm.user u " +
			"WHERE w.publicId = :workspacePublicId AND u.publicId = :userPublicId")
	Optional<WorkspaceMember> findByUserAndWorkspacePublicId(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("userPublicId") UUID userPublicId
	);

	@Query("SELECT wm FROM WorkspaceMember wm " +
			"JOIN FETCH wm.user u " +
			"WHERE wm.workspace.publicId = :workspacePublicId " +
			"AND u.publicId IN (:requesterUserPublicId, :targetMemberUserPublicId)")
	List<WorkspaceMember> findMembersForUpdate(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("requesterUserPublicId") UUID requesterUserPublicId,
			@Param("targetMemberUserPublicId")UUID targetMemberUserPublicId
	);

	@Query("SELECT wm FROM WorkspaceMember wm " +
			"JOIN FETCH wm.user u " +
			"JOIN FETCH wm.workspace w " +
			"WHERE w.publicId = :workspacePublicId " +
			"AND u.publicId IN (:requesterUserPublicId, :targetMemberUserPublicId)")
	List<WorkspaceMember> findMembersWithWorkspace(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("requesterUserPublicId") UUID requesterUserPublicId,
			@Param("targetMemberUserPublicId")UUID targetMemberUserPublicId
	);

	@Query("SELECT COUNT(wm) > 0 " +
			"FROM WorkspaceMember wm " +
			"WHERE wm.workspace.publicId = :workspacePublicId AND wm.user.email = :email")
	boolean existsByWorkspacePublicIdAndUserEmail(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("email") String email
	);

	@Query("SELECT COUNT(wm) > 0 " +
			"FROM WorkspaceMember wm " +
			"WHERE wm.workspace.publicId = :workspacePublicId AND wm.user.publicId = :userPublicId")
	boolean existsByWorkspacePublicIdAndUserPublicId(
			@Param("workspacePublicId") UUID workspacePublicId,
			@Param("userPublicId") UUID userPublicId
	);

	boolean existsByWorkspaceAndUser(Workspace workspace, User user);
}
