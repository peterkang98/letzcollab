package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.WorkspaceInvitation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

	@Query("SELECT wi " +
			"FROM WorkspaceInvitation wi " +
			"JOIN FETCH wi.workspace " +
			"WHERE wi.token = :token")
	Optional<WorkspaceInvitation> findByTokenWithWorkspace(@Param("token") UUID token);

	long countByInviterAndCreatedAtAfter(User inviter, LocalDateTime since);

	@Modifying
	@Query("DELETE FROM WorkspaceInvitation w WHERE w.createdAt < :cutoff")
	int deleteInvitationsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
