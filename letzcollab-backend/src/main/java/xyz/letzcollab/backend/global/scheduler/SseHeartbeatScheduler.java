package xyz.letzcollab.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.global.sse.SseEmitterService;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {
	private final SseEmitterService sseEmitterService;

	@Scheduled(fixedRate = 30000)
	public void triggerHeartbeat() {
		Set<UUID> userIds = sseEmitterService.getActiveUserIds();
		if (userIds.isEmpty()) return;

		log.debug("정기 SSE 하트비트 스케줄러 가동 - 대상 유저 수: {}명", userIds.size());
		userIds.forEach(sseEmitterService::sendHeartbeat);
	}
}
