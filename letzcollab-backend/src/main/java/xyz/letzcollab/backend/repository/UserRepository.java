package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.letzcollab.backend.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByPublicId(UUID publicId);

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);
}
