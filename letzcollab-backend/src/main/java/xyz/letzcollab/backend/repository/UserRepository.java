package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.letzcollab.backend.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByPublicId(String publicId);

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);
}
