package xyz.letzcollab.backend.global.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.entity.vo.UserStatus;
import xyz.letzcollab.backend.global.security.jwt.JwtTokenProvider;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

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

	@Nested
	@DisplayName("인증 없이 접근 가능한 경로")
	class PermitAllPaths {

		@Test
		@DisplayName("POST /api/v1/auth/** 는 인증 없이 접근 가능하다")
		void authEndpoint_noTokenRequired() throws Exception {
			mockMvc.perform(post("/api/v1/auth/login"))
				   .andExpect(status().is(not(is(401))))
				   .andExpect(status().is(not(is(403))));
		}

		@Test
		@DisplayName("GET /api/v1/auth/** 는 인증 없이 접근 가능하다")
		void authEndpointGet_noTokenRequired() throws Exception {
			mockMvc.perform(get("/api/v1/auth/check"))
				   .andExpect(status().is(not(is(401))))
				   .andExpect(status().is(not(is(403))));
		}
	}

	@Nested
	@DisplayName("인증이 필요한 경로")
	class ProtectedPaths {

		@Test
		@DisplayName("JWT 없이 보호된 경로에 접근하면 401을 반환한다")
		void protectedEndpoint_withNoToken_returns401() throws Exception {
			mockMvc.perform(get("/api/v1/users/me"))
				   .andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("유효한 Bearer JWT를 넣으면 보호된 경로를 통과한다")
		void protectedEndpoint_withValidToken_passes() throws Exception {
			String token = createValidToken();

			// 404는 컨트롤러가 없어서 발생 - 보안 필터는 통과한 것
			mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				   .andExpect(status().is(not(is(401))))
				   .andExpect(status().is(not(is(403))));
		}

		@Test
		@DisplayName("유효한 accessToken 쿠키로 보호된 경로를 통과한다")
		void validCookieToken_passesFilter() throws Exception {
			String token = createValidToken();

			mockMvc.perform(get("/api/v1/users/me").cookie(new Cookie("accessToken", token)))
				   .andExpect(status().is(not(is(401))))
				   .andExpect(status().is(not(is(403))));
		}
	}


	@Nested
	@DisplayName("CORS 설정")
	class CorsConfig {

		@Test
		@DisplayName("허용된 Origin의 Preflight 요청에 200 을 반환한다")
		void cors_allowedOrigin_preflightSuccess() throws Exception {
			mockMvc.perform(options("/api/v1/auth/login")
						   .header(HttpHeaders.ORIGIN, "http://localhost:5173")
						   .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				   .andExpect(status().isOk())
				   .andExpect(header().string(
						   HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"
				   ));
		}

		@Test
		@DisplayName("허용되지 않은 Origin의 Preflight 요청에는 Access-Control-Allow-Origin 헤더가 없다")
		void cors_disallowedOrigin_noAcaoHeader() throws Exception {
			mockMvc.perform(options("/api/v1/auth/login")
						   .header(HttpHeaders.ORIGIN, "http://evil.com")
						   .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				   .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		}

		@Test
		@DisplayName("X-Client-Type 커스텀 헤더가 허용된다")
		void cors_customHeader_allowed() throws Exception {
			mockMvc.perform(options("/api/v1/auth/login")
						   .header(HttpHeaders.ORIGIN, "http://localhost:5173")
						   .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						   .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Client-Type"))
				   .andExpect(status().isOk());
		}
	}


	@Nested
	@DisplayName("세션 정책")
	class SessionPolicy {

		@Test
		@DisplayName("응답에 Set-Cookie 세션 헤더가 없다 (STATELESS)")
		void session_stateless_noSessionCookie() throws Exception {
			mockMvc.perform(post("/api/v1/auth/login"))
				   .andExpect(result -> {
					   String setCookie = result.getResponse().getHeader("Set-Cookie");
					   assertThat(setCookie).as("STATELESS 설정이므로 JSESSIONID 쿠키가 없어야 한다").isNullOrEmpty();
				   });
		}
	}
}