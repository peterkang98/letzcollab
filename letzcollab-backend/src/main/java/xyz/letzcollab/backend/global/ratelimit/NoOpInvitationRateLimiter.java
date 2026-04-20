package xyz.letzcollab.backend.global.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;

import java.util.UUID;

@Component
@Profile("!prod")
public class NoOpInvitationRateLimiter implements InvitationRateLimiter{
	@Override
	public void rateLimitInviteEmail(UUID userPublicId, UUID workspacePublicId, User inviter, Workspace workspace) {}
}
