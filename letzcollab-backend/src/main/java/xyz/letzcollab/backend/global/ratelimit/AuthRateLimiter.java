package xyz.letzcollab.backend.global.ratelimit;

public interface AuthRateLimiter {
	void rateLimitResetPwdReq(String email);
}
