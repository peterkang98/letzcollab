package xyz.letzcollab.backend.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.letzcollab.backend.global.security.AuthErrorHandler;
import xyz.letzcollab.backend.global.exception.ErrorCode;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final AuthErrorHandler errorHandler;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		String extractedToken = extractToken(request);

		try {
			if (StringUtils.hasText(extractedToken)) {
				Authentication authentication = jwtTokenProvider.getAuthentication(extractedToken);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			errorHandler.handle(response, ErrorCode.JWT_TOKEN_EXPIRED);
			return;
		} catch (JwtException e) {
			log.warn("JWT 검증 실패: {}", e.getMessage());
			errorHandler.handle(response, ErrorCode.JWT_INVALID_TOKEN);
			return;
		}
	}

	private String extractToken(HttpServletRequest request) {
		// 1. 헤더 체크 (모바일 앱 용)
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}

		// 2. 쿠키 체크 (브라우저 용)
		if (request.getCookies() != null) {
			return Arrays.stream(request.getCookies())
						 .filter(c -> c.getName().equals("accessToken"))
						 .map(Cookie::getValue)
						 .findFirst()
						 .orElse(null);
		}

		return null;
	}
}
