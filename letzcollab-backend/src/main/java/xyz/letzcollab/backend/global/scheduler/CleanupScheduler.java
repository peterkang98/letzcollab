package xyz.letzcollab.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.repository.NotificationRepository;
import xyz.letzcollab.backend.repository.VerificationTokenRepository;
import xyz.letzcollab.backend.repository.WorkspaceInvitationRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {
	private final VerificationTokenRepository tokenRepository;
	private final WorkspaceInvitationRepository invitationRepository;
	private final NotificationRepository notificationRepository;

	private static final int TOKEN_RETENTION_DAYS = 1;
	private static final int INVITATION_RETENTION_DAYS = 1;
	private static final int NOTIFICATION_RETENTION_DAYS = 30;

	/**
	 * 매일 새벽 4시에 실행
	 * 읽음 처리된 지 30일이 지난 알림을 벌크 삭제
	 */
	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void deleteOldReadNotifications() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(NOTIFICATION_RETENTION_DAYS);
		int deletedCount = notificationRepository.deleteReadNotificationsOlderThan(cutoff);
		log.info("오래된 알림 정리 완료 - 삭제 건수={}, 기준일시={}", deletedCount, cutoff);
	}

	/**
	 * 매일 새벽 5시에 실행
	 * 생성된 지 하루 이상 지난 (이메일/비밀번호 재설정) 인증 토큰 벌크 삭제
	 */
	@Scheduled(cron = "0 0 5 * * *")
	@Transactional
	public void deleteOldVerificationTokens() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(TOKEN_RETENTION_DAYS);
		int deletedCount = tokenRepository.deleteTokensOlderThan(cutoff);
		log.info("오래된 인증 토큰 정리 완료 - 삭제 건수={}, 기준일시={}", deletedCount, cutoff);
	}

	/**
	 * 매일 새벽 5시반에 실행
	 * 생성된 지 하루 이상 지난 워크스페이스 초대장 벌크 삭제
	 */
	@Scheduled(cron = "0 30 5 * * *")
	@Transactional
	public void deleteOldInvitations() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(INVITATION_RETENTION_DAYS);
		int deletedCount = invitationRepository.deleteInvitationsOlderThan(cutoff);
		log.info("오래된 워크스페이스 초대장 정리 완료 - 삭제 건수={}, 기준일시={}", deletedCount, cutoff);
	}
}
