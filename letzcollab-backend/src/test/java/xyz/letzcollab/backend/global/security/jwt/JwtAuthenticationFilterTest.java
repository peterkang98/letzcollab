package xyz.letzcollab.backend.global.security.jwt;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.entity.vo.UserStatus;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("JwtAuthenticationFilter 통합 테스트")
class JwtAuthenticationFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Value("${jwt.secret}")
	private String testSecretKey;

	// 401 에러 응답 검증 헬퍼
	private void expectUnauthorizedResponse(ResultActions result, ErrorCode expectedErrorCode) throws Exception {
		result
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.errorCode").value(expectedErrorCode.getCode()))
			.andExpect(jsonPath("$.message").value(expectedErrorCode.getMessage()));
	}

	private String createValidToken() {
		CustomUserDetails userDetails = new CustomUserDetails(
				"테스트유저",
				"f975142b-aae5-4747-aaa9-f7ad11d84ce3",
				"user@example.com",
				"pw",
				UserRole.USER,
				UserStatus.ACTIVE
		);
		Authentication auth = new UsernamePasswordAuthenticationToken(
				userDetails, "", userDetails.getAuthorities()
		);
		return jwtTokenProvider.createToken(auth);
	}

	// ──────────────────────────────────────────────────────────
	// 토큰 없음
	// ──────────────────────────────────────────────────────────
	@Nested
	@DisplayName("토큰이 없는 요청")
	class NoToken {
		@Test
		@DisplayName("토큰 없이 보호된 경로에 접근하면 401과 UNAUTHORIZED 에러 응답을 반환한다")
		void noToken_returns401_withUnauthorizedErrorResponse() throws Exception {
			expectUnauthorizedResponse(
					mockMvc.perform(get("/api/v1/users/me")),
					ErrorCode.UNAUTHORIZED
			);
		}
	}


	@Nested
	@DisplayName("Authorization 헤더 Bearer 토큰")
	class BearerHeaderToken {

		@Test
		@DisplayName("유효한 Bearer 토큰으로 보호된 경로를 통과한다")
		void validBearerToken_passesFilter() throws Exception {
			String token = createValidToken();

			mockMvc.perform(get("/api/v1/users/me")
						   .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				   .andExpect(status().is(not(is(401))))
				   .andExpect(status().is(not(is(403))));
		}

		@Test
		@DisplayName("Bearer 접두사 없는 헤더는 토큰으로 인식하지 않아 401과 UNAUTHORIZED 에러 응답을 반환한다")
		void noBearerPrefix_returns401() throws Exception {
			String token = createValidToken();

			expectUnauthorizedResponse(
					mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, token)),
					ErrorCode.UNAUTHORIZED
			);
		}
	}


	@Nested
	@DisplayName("쿠키 accessToken")
	class CookieToken {

		@Test
		@DisplayName("accessToken 쿠키로 보호된 경로를 통과한다")
		void validCookieToken_passesFilter() throws Exception {
			String token = createValidToken();

			mockMvc.perform(get("/api/v1/users/me")
						   .cookie(new Cookie("accessToken", token)))
				   .andExpect(status().is(not(is(401))))
				   .andExpect(status().is(not(is(403))));
		}

		@Test
		@DisplayName("이름이 다른 쿠키는 토큰으로 인식하지 않아 401과 UNAUTHORIZED 에러 응답을 반환한다")
		void wrongCookieName_returns401_withUnauthorizedErrorResponse() throws Exception {
			String token = createValidToken();

			expectUnauthorizedResponse(
					mockMvc.perform(get("/api/v1/users/me")
							.cookie(new Cookie("otherCookie", token))),
					ErrorCode.UNAUTHORIZED
			);
		}
	}


	@Nested
	@DisplayName("만료된 토큰")
	class ExpiredToken {

		@Test
		@DisplayName("만료된 토큰이면 401과 JWT_TOKEN_EXPIRED 에러 응답을 반환한다")
		void expiredToken_returns401_withExpiredErrorResponse() throws Exception {
			// validity = -1ms 로 즉시 만료되는 토큰 생성
			JwtTokenProvider expiredProvider = new JwtTokenProvider(testSecretKey, -1L);
			CustomUserDetails userDetails = new CustomUserDetails(
					"테스트",
					"f975142b-aae5-4747-aaa9-f7ad11d84ce3",
					"user@example.com",
					"pw",
					UserRole.USER,
					UserStatus.ACTIVE
			);
			Authentication auth = new UsernamePasswordAuthenticationToken(
					userDetails, "", userDetails.getAuthorities()
			);
			String expiredToken = expiredProvider.createToken(auth);

			expectUnauthorizedResponse(
					mockMvc.perform(get("/api/v1/users/me")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)),
					ErrorCode.JWT_TOKEN_EXPIRED
			);
		}
	}


	@Nested
	@DisplayName("변조/잘못된 토큰")
	class InvalidToken {

		@Test
		@DisplayName("변조된 토큰이면 401과 JWT_INVALID_TOKEN 에러 응답을 반환한다")
		void tamperedToken_returns401_withInvalidTokenErrorResponse() throws Exception {
			String token = createValidToken();
			String tampered = token.substring(0, token.length() - 5) + "XXXXX";

			expectUnauthorizedResponse(
					mockMvc.perform(get("/api/v1/users/me")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered)),
					ErrorCode.JWT_INVALID_TOKEN
			);
		}

		@Test
		@DisplayName("완전히 잘못된 문자열이면 401과 JWT_INVALID_TOKEN 에러 응답을 반환한다")
		void randomString_returns401_withInvalidTokenErrorResponse() throws Exception {
			expectUnauthorizedResponse(
					mockMvc.perform(get("/api/v1/users/me")
							.header(HttpHeaders.AUTHORIZATION, "Bearer XXXXX")),
					ErrorCode.JWT_INVALID_TOKEN
			);
		}
	}
}