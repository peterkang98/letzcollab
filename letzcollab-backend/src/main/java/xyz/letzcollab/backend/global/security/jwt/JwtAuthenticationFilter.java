package xyz.letzcollab.backend.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.letzcollab.backend.global.dto.ApiResponse;
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
	private final ObjectMapper objectMapper;

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
			sendErrorResponse(response, ErrorCode.JWT_TOKEN_EXPIRED);
			return;
		} catch (JwtException e) {
			log.warn("JWT 검증 실패: {}", e.getMessage());
			sendErrorResponse(response, ErrorCode.JWT_INVALID_TOKEN);
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

	private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Void> apiResponse = ApiResponse.fail(errorCode);

		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
	}
}
