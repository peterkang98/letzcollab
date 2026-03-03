package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.letzcollab.backend.entity.VerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
	Optional<VerificationToken> findByToken(UUID token);
}
