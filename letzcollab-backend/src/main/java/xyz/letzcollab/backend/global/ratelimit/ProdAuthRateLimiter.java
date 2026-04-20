package xyz.letzcollab.backend.global.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.entity.vo.TokenType;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.repository.VerificationTokenRepository;

import java.time.LocalDateTime;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProdAuthRateLimiter implements AuthRateLimiter{

	private final VerificationTokenRepository tokenRepository;

	// 24시간에 3회 제한
	@Override
	public void rateLimitResetPwdReq(String email) {
		long count = tokenRepository.countRecentByEmailAndType(
				email, TokenType.PASSWORD_RESET, LocalDateTime.now().minusHours(24)
		);

		if (count >= 3) {
			log.warn("비밀번호 재설정 요청 rate limit 초과 - email: {}", email);
			throw new CustomException(ErrorCode.EMAIL_SEND_TOO_FREQUENT);
		}
	}
}
