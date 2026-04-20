package xyz.letzcollab.backend.global.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.entity.User;

@Component
@Profile("!prod")
public class NoOpInvitationRateLimiter implements InvitationRateLimiter{
	@Override
	public void rateLimitInviteEmail(User inviter) {}
}
