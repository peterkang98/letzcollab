package xyz.letzcollab.backend.repository.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import xyz.letzcollab.backend.dto.auth.RefreshTokenData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("RefreshTokenRepository 단위 테스트")
class RefreshTokenRepositoryTest {

	@Container
	private static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	private RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	void setUp() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
				redis.getHost(),
				redis.getFirstMappedPort()
		);
		LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
		factory.afterPropertiesSet();

		RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.afterPropertiesSet();

		refreshTokenRepository = new RefreshTokenRepository(redisTemplate, 14L, new ObjectMapper());
	}

	private RefreshTokenData createTokenData() {
		return new RefreshTokenData(
				UUID.randomUUID().toString(),
				"test@example.com",
				"ROLE_USER"
		);
	}

	@Test
	@DisplayName("저장한 갱신 토큰을 올바르게 조회할 수 있다")
	void saveAndGet_success() {
		// given
		String token = "test-token";
		RefreshTokenData data = createTokenData();

		// when
		refreshTokenRepository.saveRefreshToken(token, data);
		RefreshTokenData found = refreshTokenRepository.getRefreshTokenData(token);

		// then
		assertThat(found).isNotNull();
		assertThat(found.publicId()).isEqualTo(data.publicId());
		assertThat(found.email()).isEqualTo(data.email());
		assertThat(found.role()).isEqualTo(data.role());
	}

	@Test
	@DisplayName("존재하지 않는 토큰 조회 시 null을 반환한다")
	void get_nonexistent_returnsNull() {
		assertThat(refreshTokenRepository.getRefreshTokenData("nonexistent")).isNull();
	}

	@Test
	@DisplayName("갱신 토큰 삭제 후 조회 시 null을 반환한다")
	void delete_success() {
		// given
		String token = "delete-test-token";
		refreshTokenRepository.saveRefreshToken(token, createTokenData());

		// when
		boolean deleted = refreshTokenRepository.deleteRefreshToken(token);

		// then
		assertThat(deleted).isTrue();
		assertThat(refreshTokenRepository.getRefreshTokenData(token)).isNull();
	}

	@Test
	@DisplayName("존재하지 않는 토큰 삭제 시 false를 반환한다")
	void delete_nonexistent_returnsFalse() {
		assertThat(refreshTokenRepository.deleteRefreshToken("nonexistent")).isFalse();
	}
}