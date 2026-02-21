package xyz.letzcollab.backend.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;

	// 테스트용 Base64 인코딩된 시크릿 키
	private static final String TEST_SECRET = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
	private static final long VALID_EXPIRY_MS = 3_600_000L;   // 1시간
	private static final long EXPIRED_EXPIRY_MS = -1L;        // 즉시 만료

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, VALID_EXPIRY_MS);
	}

	@Nested
	@DisplayName("토큰 생성")
	class CreateToken {

		@Test
		@DisplayName("유효한 Authentication으로 토큰을 생성한다")
		void createToken_success() {
			Authentication authentication = mockAuthentication("f975142b-aae5-4747-aaa9-f7ad11d84ce3", "user@example.com", UserRole.USER);

			String token = jwtTokenProvider.createToken(authentication);

			assertThat(token).isNotBlank();
			assertThat(token.split("\\.")).hasSize(3); // JWT 형식 검증 (header.payload.signature)
		}

		@Test
		@DisplayName("생성된 토큰에 publicId, email, role이 포함된다")
		void createToken_containsClaims() {
			String publicId = "f975142b-aae5-4747-aaa9-f7ad11d84ce3";
			String email = "user@example.com";
			UserRole role = UserRole.USER;

			Authentication authentication = mockAuthentication(publicId, email, role);
			String token = jwtTokenProvider.createToken(authentication);

			// 토큰을 다시 파싱해 클레임 검증
			Authentication parsed = jwtTokenProvider.getAuthentication(token);
			CustomUserDetails details = (CustomUserDetails) parsed.getPrincipal();

			assertThat(details.getPublicId()).isEqualTo(publicId);
			assertThat(details.getUsername()).isEqualTo(email);
			assertThat(details.getRole()).isEqualTo(role);
		}
	}

	@Nested
	@DisplayName("토큰을 사용해서 Authentication 객체 생성")
	class GetAuthentication {

		@Test
		@DisplayName("유효한 토큰으로 Authentication 객체를 반환한다")
		void getAuthentication_success() {
			Authentication authentication = mockAuthentication("f975142b-aae5-4747-aaa9-f7ad11d84ce3", "admin@example.com", UserRole.ADMIN);
			String token = jwtTokenProvider.createToken(authentication);

			Authentication result = jwtTokenProvider.getAuthentication(token);

			assertThat(result).isNotNull();
			assertThat(result.isAuthenticated()).isTrue();
		}

		@Test
		@DisplayName("반환된 Authentication의 Principal이 CustomUserDetails이다")
		void getAuthentication_principalType() {
			Authentication authentication = mockAuthentication("f975142b-aae5-4747-aaa9-f7ad11d84ce3", "user@example.com", UserRole.USER);
			String token = jwtTokenProvider.createToken(authentication);

			Authentication result = jwtTokenProvider.getAuthentication(token);

			assertThat(result.getPrincipal()).isInstanceOf(CustomUserDetails.class);
		}

		@Test
		@DisplayName("반환된 Authentication의 권한에 role이 포함된다")
		void getAuthentication_containsAuthority() {
			Authentication authentication = mockAuthentication("f975142b-aae5-4747-aaa9-f7ad11d84ce3", "user@example.com", UserRole.USER);
			String token = jwtTokenProvider.createToken(authentication);

			Authentication result = jwtTokenProvider.getAuthentication(token);

			assertThat(result.getAuthorities())
					.anyMatch(a -> a.getAuthority().equals(UserRole.USER.getAuthority()));
		}

		@Test
		@DisplayName("만료된 토큰이면 ExpiredJwtException이 발생한다")
		void getAuthentication_expiredToken() {
			JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, EXPIRED_EXPIRY_MS);
			Authentication authentication = mockAuthentication("f975142b-aae5-4747-aaa9-f7ad11d84ce3", "user@example.com", UserRole.USER);
			String expiredToken = expiredProvider.createToken(authentication);

			assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(expiredToken))
					.isInstanceOf(ExpiredJwtException.class);
		}

		@Test
		@DisplayName("변조된 토큰이면 JwtException이 발생한다")
		void getAuthentication_tamperedToken() {
			Authentication authentication = mockAuthentication("f975142b-aae5-4747-aaa9-f7ad11d84ce3", "user@example.com", UserRole.USER);
			String token = jwtTokenProvider.createToken(authentication);
			String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

			assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tamperedToken))
					.isInstanceOf(JwtException.class);
		}

		@Test
		@DisplayName("완전히 잘못된 문자열이면 JwtException이 발생한다")
		void getAuthentication_invalidToken() {
			assertThatThrownBy(() -> jwtTokenProvider.getAuthentication("not.a.jwt"))
					.isInstanceOf(JwtException.class);
		}
	}


	private Authentication mockAuthentication(String publicId, String email, UserRole role) {
		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.getPublicId()).thenReturn(publicId);
		when(userDetails.getUsername()).thenReturn(email);
		when(userDetails.getRole()).thenReturn(role);

		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		return authentication;
	}
}
