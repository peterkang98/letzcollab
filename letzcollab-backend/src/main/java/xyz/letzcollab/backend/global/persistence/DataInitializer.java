package xyz.letzcollab.backend.global.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.repository.UserRepository;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;

	@Value("${users.admin.password}")
	private String adminPassword;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		User dummyUser = User.createDummyUser(
				"홍길동",
				"honggildong@naver.com",
				passwordEncoder.encode("123456"),
				"010-2345-4321"
		);

		User dummyUser2 = User.createDummyUser(
				"peter",
				"peter8790@naver.com",
				passwordEncoder.encode("123456"),
				"010-8765-4321"
		);

		User adminUser = User.createAdminUser(
				"admin",
				"admin@letzcollab.xyz",
				passwordEncoder.encode(adminPassword),
				"010-1234-5678"
		);

		userRepository.save(dummyUser);
		userRepository.save(dummyUser2);
		userRepository.save(adminUser);
	}
}
