package xyz.letzcollab.backend.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {
	private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

	public SseEmitter subscribe(UUID userPublicId) {
		// 기존 연결이 있으면 종료 후 제거
		SseEmitter oldEmitter = emitters.remove(userPublicId);

		if (oldEmitter != null) {
			oldEmitter.complete();
		}

		SseEmitter emitter = new SseEmitter(10 * 60 * 60 * 1000L);

		emitter.onCompletion(() -> {
			emitters.remove(userPublicId, emitter);
			log.debug("SSE 연결 종료 완료 - userId={}", userPublicId);
		});
		emitter.onError(throwable -> {
			emitters.remove(userPublicId, emitter);
			log.debug("SSE 연결 오류 - userId={}, error={}", userPublicId, throwable.getMessage());
		});
		emitter.onTimeout(() -> {
			emitters.remove(userPublicId, emitter);
			log.debug("SSE 연결 타임아웃 - userId={}", userPublicId);
		});

		emitters.put(userPublicId, emitter);
		log.debug("SSE 구독 등록 - userId={}, total connections={}", userPublicId, emitters.size());
		return emitter;
	}

	/**
	 * 특정 사용자에게 SSE 이벤트 전송
	 */
	public void send(UUID userPublicId, String eventName, Object data) {
		SseEmitter emitter = emitters.get(userPublicId);

		if (emitter == null) {
			return; // 연결되지 않은 사용자는 무시
		}

		try {
			emitter.send(SseEmitter.event()
								   .name(eventName)
								   .data(data));
		} catch (IOException e) {
			log.debug("SSE 전송 실패 - userId={}, error={}", userPublicId, e.getMessage());
			emitters.remove(userPublicId, emitter);
		}
	}

	@Async("sseHeartbeatExecutor")
	public void sendHeartbeat(UUID userPublicId) {
		SseEmitter emitter = emitters.get(userPublicId);
		if (emitter == null) return;

		try {
			emitter.send(SseEmitter.event().comment("ping"));
		} catch (IOException e) {
			log.debug("하트비트 전송 실패로 연결 제거 - userId={}", userPublicId);
			emitters.remove(userPublicId, emitter);
		}
	}

	public Set<UUID> getActiveUserIds() {
		return emitters.keySet();
	}

	public boolean isSubscribed(UUID userPublicId) {
		return emitters.containsKey(userPublicId);
	}

	// 테스트용
//	public void complete(UUID userPublicId) {
//		SseEmitter emitter = emitters.get(userPublicId);
//		if (emitter != null) {
//			emitter.complete();
//		}
//	}
}
