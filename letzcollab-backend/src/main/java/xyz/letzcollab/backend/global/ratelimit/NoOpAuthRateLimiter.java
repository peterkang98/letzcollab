package xyz.letzcollab.backend.global.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class NoOpAuthRateLimiter implements AuthRateLimiter{
	@Override
	public void rateLimitResetPwdReq(String email) {}
}
