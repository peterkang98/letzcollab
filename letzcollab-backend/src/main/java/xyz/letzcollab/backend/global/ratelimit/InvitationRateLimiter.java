package xyz.letzcollab.backend.global.ratelimit;

import xyz.letzcollab.backend.entity.User;

public interface InvitationRateLimiter {
	void rateLimitInviteEmail(User inviter);
}
