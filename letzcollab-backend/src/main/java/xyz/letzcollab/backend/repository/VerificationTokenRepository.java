package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.VerificationToken;
import xyz.letzcollab.backend.entity.vo.TokenType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
	Optional<VerificationToken> findByToken(UUID token);

	@Query("SELECT COUNT(t) FROM VerificationToken t " +
			"WHERE t.user.email = :email AND t.type = :type AND t.createdAt > :since")
	long countRecentByEmailAndType(
			@Param("email") String email,
			@Param("type") TokenType type,
			@Param("since") LocalDateTime since
	);
}
