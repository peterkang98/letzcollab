package xyz.letzcollab.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

@TestConfiguration
public class TestAuditConfig {
	@Bean
	@Primary // 테스트 시 실제 구현체보다 우선순위를 가짐
	public AuditorAware<UUID> testAuditorAware() {
		// 테스트에서 사용할 고정 UUID 반환
		return () -> Optional.of(UUID.fromString("f975142b-aae5-4747-aaa9-f7ad11d84ce3"));
	}
}
