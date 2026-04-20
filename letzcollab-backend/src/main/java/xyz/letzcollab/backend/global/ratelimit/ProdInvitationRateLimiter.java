package xyz.letzcollab.backend.global.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.repository.WorkspaceInvitationRepository;

import java.time.LocalDateTime;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProdInvitationRateLimiter implements InvitationRateLimiter{

	private final WorkspaceInvitationRepository invitationRepository;

	// 1시간에 50개로 제한
	@Override
	public void rateLimitInviteEmail(User inviter) {
		long count = invitationRepository.countByInviterAndCreatedAtAfter(inviter, LocalDateTime.now().minusHours(1));

		if (count >= 50) {
			log.warn("워크스페이스 초대 rate limit 초과 - inviterUserId={}", inviter.getPublicId());
			throw new CustomException(ErrorCode.EMAIL_SEND_TOO_FREQUENT);
		}
	}
}
