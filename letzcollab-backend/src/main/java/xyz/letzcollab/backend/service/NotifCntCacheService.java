package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import xyz.letzcollab.backend.repository.NotificationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifCntCacheService {
	private static final String NOTIF_COUNT_PREFIX = "NT:COUNT:";

	private final StringRedisTemplate redisTemplate;
	private final NotificationRepository notificationRepository;

	/**
	 * 미확인 알림 개수 조회
	 * - Redis에 키가 있으면 GET
	 * - 키가 없으면 DB COUNT → SET → 반환
	 */
	public long get(UUID userPublicId) {
		String key = buildKey(userPublicId);
		String value = redisTemplate.opsForValue().get(key);

		if (value != null) {
			return Long.parseLong(value);
		}

		// 캐시 미스 -> DB에서 로드
		long count = notificationRepository.countByRecipientPublicIdAndIsReadFalse(userPublicId);
		redisTemplate.opsForValue().set(key, String.valueOf(count));
		log.debug("unreadCount 캐시 초기화 - userId={}, count={}", userPublicId, count);
		return count;
	}

	/**
	 * 알림 생성 후 호출 — Redis INCR
	 * 키가 없으면 무시 (다음 get()에서 DB 조회)
	 */
	public void increment(UUID userPublicId) {
		String key = buildKey(userPublicId);
		Boolean exists = redisTemplate.hasKey(key);

		if (Boolean.TRUE.equals(exists)) {
			Long result = redisTemplate.opsForValue().increment(key);
			log.debug("unreadCount INCR - userId={}, result={}", userPublicId, result);
		}
	}

	/**
	 * 단건 읽음 처리 후 호출 — Redis DECR
	 * 키가 없으면 무시 (다음 get()에서 DB 조회)
	 */
	public void decrement(UUID userPublicId) {
		String key = buildKey(userPublicId);
		Boolean exists = redisTemplate.hasKey(key);

		if (Boolean.TRUE.equals(exists)) {
			Long result = redisTemplate.opsForValue().decrement(key);
			// 0 미만 방지
			if (result != null && result < 0) {
				redisTemplate.opsForValue().set(key, "0");
			}
			log.debug("unreadCount DECR - userId={}, result={}", userPublicId, result);
		}
	}

	/**
	 * 전체 읽음 처리 후 호출 — Redis SET 0
	 */
	public void resetToZero(UUID userPublicId) {
		redisTemplate.opsForValue().set(buildKey(userPublicId), "0");
		log.debug("unreadCount RESET - userId={}", userPublicId);
	}

	private String buildKey(UUID userPublicId) {
		return NOTIF_COUNT_PREFIX + userPublicId;
	}
}
