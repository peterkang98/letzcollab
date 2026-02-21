package xyz.letzcollab.backend.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {
	private final SecretKey key;
	private final long accessTokenValidityInMs;

	public JwtTokenProvider(
			@Value("${jwt.secret}") String secretKey,
			@Value("${jwt.access-validity-in-ms}") long accessTokenValidityInMs
	) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.accessTokenValidityInMs = accessTokenValidityInMs;
	}

	public String createToken(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		String publicId = userDetails.getPublicId();
		String email = userDetails.getUsername();
		String role = userDetails.getRole().getAuthority();

		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + accessTokenValidityInMs);

		return Jwts.builder()
				   .subject(publicId)
				   .claim("email", email)
				   .claim("role", role)
				   .issuedAt(now)
				   .expiration(expiryDate)
				   .signWith(key)
				   .compact();
	}

	public Authentication getAuthentication(String extractedToken) {
		Claims claims = generateClaims(extractedToken);

		String publicId = claims.getSubject();
		String email = claims.get("email", String.class);
		String role = claims.get("role", String.class);

		CustomUserDetails userDetails = new CustomUserDetails("", publicId, email, "", UserRole.fromAuthority(role), null);

		return new UsernamePasswordAuthenticationToken(userDetails, "", List.of(new SimpleGrantedAuthority(role)));
	}

	private Claims generateClaims(String extractedToken) {
		return Jwts.parser()
				   .verifyWith(key)
				   .build()
				   .parseSignedClaims(extractedToken)
				   .getPayload();
	}
}
