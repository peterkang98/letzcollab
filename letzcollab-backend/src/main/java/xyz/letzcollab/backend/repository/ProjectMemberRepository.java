package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
	boolean existsByProjectPublicIdAndUserPublicIdAndRole(UUID projectPublicId, UUID userPublicId, ProjectRole role);

	boolean existsByProjectPublicIdAndUserPublicId(UUID projectPublicId, UUID userPublicId);

	Optional<ProjectMember> findByUserPublicIdAndProjectPublicId(UUID userPublicId, UUID projectPublicId);

	@Query("SELECT pm FROM ProjectMember pm " +
			"JOIN FETCH pm.project p " +
			"JOIN FETCH p.leader l " +
			"WHERE p.publicId = :projectPublicId AND pm.user.publicId = :userPublicId")
	Optional<ProjectMember> findMemberWithProjectAndLeader(
			@Param("projectPublicId") UUID projectPublicId,
			@Param("userPublicId") UUID userPublicId
	);

	@Query("SELECT pm FROM ProjectMember pm " +
			"JOIN FETCH pm.project p " +
			"JOIN FETCH p.leader " +
			"JOIN FETCH pm.user u " +
			"WHERE p.publicId = :projectPublicId " +
			"AND u.publicId IN (:requesterPublicId, :targetUserPublicId)")
	List<ProjectMember> findMembersWithProjectAndLeader(
			@Param("projectPublicId") UUID projectPublicId,
			@Param("requesterPublicId") UUID requesterPublicId,
			@Param("targetUserPublicId") UUID targetUserPublicId
	);
}
