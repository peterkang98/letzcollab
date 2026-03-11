package xyz.letzcollab.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.repository.NotificationRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {
	private final NotificationRepository notificationRepository;

	private static final int RETENTION_DAYS = 30;

	/**
	 * 매일 새벽 4시에 실행
	 * 읽음 처리된 지 30일이 지난 알림을 벌크 삭제
	 */
	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void deleteOldReadNotifications() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
		int deletedCount = notificationRepository.deleteReadNotificationsOlderThan(cutoff);
		log.info("오래된 알림 정리 완료 - 삭제 건수={}, 기준일시={}", deletedCount, cutoff);
	}
}
