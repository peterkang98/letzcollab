package xyz.letzcollab.backend.dto.auth;

import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;

public record RefreshTokenData(String publicId, String email, String role) {
	public static RefreshTokenData from(CustomUserDetails userDetails) {
		return new RefreshTokenData(
				userDetails.getPublicId().toString(),
				userDetails.getEmail(),
				userDetails.getRole().getAuthority()
		);
	}
}
