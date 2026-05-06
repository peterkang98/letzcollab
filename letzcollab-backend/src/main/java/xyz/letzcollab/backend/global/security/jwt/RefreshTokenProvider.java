package xyz.letzcollab.backend.global.security.jwt;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class RefreshTokenProvider {
	private final SecureRandom secureRandom;

	public RefreshTokenProvider() {
		this.secureRandom = new SecureRandom();
	}

	public String createToken() {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}
}
