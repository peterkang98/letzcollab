package xyz.letzcollab.backend.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import xyz.letzcollab.backend.dto.auth.RefreshTokenData;
import xyz.letzcollab.backend.global.exception.CustomException;

import java.util.concurrent.TimeUnit;

import static xyz.letzcollab.backend.global.exception.ErrorCode.DAO_ERROR;

@Repository
@Slf4j
public class RefreshTokenRepository {
	private static final String REFRESH_TOKEN_PREFIX = "RT:";

	private final RedisTemplate<String, String> redisTemplate;
	private final long rtValidityInDays;
	private final ObjectMapper objectMapper;

	public RefreshTokenRepository(
			RedisTemplate<String, String> redisTemplate,
			@Value("${jwt.refresh-validity-in-days}") long rtValidityInDays,
			ObjectMapper objectMapper
	) {
		this.redisTemplate = redisTemplate;
		this.rtValidityInDays = rtValidityInDays;
		this.objectMapper = objectMapper;
	}

	public void saveRefreshToken(String token, RefreshTokenData data) {
		try {
			String redisKey = REFRESH_TOKEN_PREFIX + token;
			String json = objectMapper.writeValueAsString(data);
			redisTemplate.opsForValue().set(redisKey, json, rtValidityInDays, TimeUnit.DAYS);
		} catch (JsonProcessingException e) {
			log.error("갱신 토큰 redis 값 직렬화 실패: {}", e.getMessage());
			throw new CustomException(DAO_ERROR);
		}
	}

	public RefreshTokenData getRefreshTokenData(String token) {
		try {
			String redisKey = REFRESH_TOKEN_PREFIX + token;
			String json = redisTemplate.opsForValue().get(redisKey);

			if (json == null) return null;
			return objectMapper.readValue(json, RefreshTokenData.class);
		} catch (JsonProcessingException e) {
			log.error("갱신 토큰 역직렬화 실패: {}", e.getMessage());
			throw new CustomException(DAO_ERROR);
		}
	}

	public boolean deleteRefreshToken(String token) {
		String redisKey = REFRESH_TOKEN_PREFIX + token;
		return Boolean.TRUE.equals(redisTemplate.delete(redisKey));
	}
}
