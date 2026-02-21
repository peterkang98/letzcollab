package xyz.letzcollab.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.auth.LoginResponse;
import xyz.letzcollab.backend.dto.auth.SignupRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.VerificationToken;
import xyz.letzcollab.backend.entity.vo.TokenType;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.entity.vo.UserStatus;
import xyz.letzcollab.backend.global.email.EmailService;
import xyz.letzcollab.backend.global.email.context.PasswordResetEmailContext;
import xyz.letzcollab.backend.global.email.context.VerifyEmailContext;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.VerificationTokenRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService 통합 테스트")
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VerificationTokenRepository tokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private EmailService emailService;


	private SignupRequest createSignupRequest(String email) {
		return new SignupRequest("홍길동", email, "Password1!", "010-1234-5678");
	}

	private User saveActiveUser(String email) {
		User user = User.createDummyUser("홍길동", email, "encodedPassword", "010-1234-5678");
		return userRepository.save(user);
	}

	// VerificationToken의 expiresAt을 강제로 만료시키는 헬퍼
	private void expireToken(VerificationToken token) throws Exception {
		Field expiresAtField = VerificationToken.class.getDeclaredField("expiresAt");
		expiresAtField.setAccessible(true);
		expiresAtField.set(token, LocalDateTime.now().minusMinutes(1));
		tokenRepository.save(token);
	}

	@Nested
	@DisplayName("회원가입")
	class Signup {

		@Test
		@DisplayName("정상 회원가입 시 PENDING 상태 유저와 이메일 인증 토큰이 생성되고 이메일이 발송된다")
		void signup_success() {
			// given
			SignupRequest request = createSignupRequest("new@example.com");

			// when
			authService.signup(request);

			// then
			User savedUser = userRepository.findByEmail("new@example.com").orElseThrow();
			assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
			assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
			assertThat(savedUser.getName()).isEqualTo("홍길동");

			List<VerificationToken> tokens = tokenRepository.findAll()
														  .stream()
														  .filter(token -> token.getUser().getId().equals(savedUser.getId()))
														  .toList();
			assertThat(tokens).hasSize(1);
			assertThat(tokens.getFirst().getType()).isEqualTo(TokenType.VERIFY_EMAIL);
			assertThat(tokens.getFirst().getUsedAt()).isNull();

			verify(emailService, times(1)).sendTemplateEmail(eq("new@example.com"), any(VerifyEmailContext.class));
		}

		@Test
		@DisplayName("이미 존재하는 이메일로 회원가입하면 DUPLICATE_EMAIL 예외가 발생한다")
		void signup_duplicateEmail_throwsException() {
			// given
			saveActiveUser("dup@example.com");
			SignupRequest request = createSignupRequest("dup@example.com");

			// when & then
			assertThatThrownBy(() -> authService.signup(request))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));

			verify(emailService, never()).sendTemplateEmail(anyString(), any(VerifyEmailContext.class));
		}

		@Test
		@DisplayName("전화번호가 null 또는 빈 값이면 null로 저장된다")
		void signup_nullPhoneNumber_savedAsNull() {
			// given
			SignupRequest request = new SignupRequest("홍길동", "nophone@example.com", "Password1!", "");

			// when
			authService.signup(request);

			// then
			User savedUser = userRepository.findByEmail("nophone@example.com").orElseThrow();
			assertThat(savedUser.getPhoneNumber()).isNull();
		}
	}

	@Nested
	@DisplayName("로그인")
	class Login {

		@BeforeEach
		void save_user() {
			authService.signup(createSignupRequest("user@example.com"));
		}

		@Test
		@DisplayName("이메일 인증을 한 사용자가 올바른 비밀번호으로 로그인하면 JWT와 사용자 정보를 반환한다")
		void login_activeUser_success() {
			// given
			User pendingUser = userRepository.findByEmail("user@example.com").orElseThrow();
			pendingUser.verifyEmail();

			// when
			LoginResponse response = authService.login("user@example.com", "Password1!");

			// then
			assertThat(response).isNotNull();
			assertThat(response.email()).isEqualTo("user@example.com");
			assertThat(response.name()).isEqualTo("홍길동");
			assertThat(response.accessToken()).isNotBlank();
		}

		@Test
		@DisplayName("잘못된 비밀번호로 로그인하면 BadCredentialsException이 발생한다")
		void login_wrongPassword_throwsBadCredentials() {
			// given
			User pendingUser = userRepository.findByEmail("user@example.com").orElseThrow();
			pendingUser.verifyEmail();

			// when & then
			assertThatThrownBy(() -> authService.login("user@example.com", "WrongPassword!"))
					.isInstanceOf(BadCredentialsException.class);
		}

		@Test
		@DisplayName("이메일 미인증(PENDING) 유저가 로그인하면 DisabledException이 발생한다")
		void login_pendingUser_throwsDisabled() {
			// when & then
			assertThatThrownBy(() -> authService.login("user@example.com", "Password1!"))
					.isInstanceOf(DisabledException.class);
		}

		@Test
		@DisplayName("탈퇴 계정으로 로그인하면 BadCredentialsException가 발생한다")
		void login_deletedUser_throwsLocked() {
			// given
			User pendingUser = userRepository.findByEmail("user@example.com").orElseThrow();
			pendingUser.delete(); // 탈퇴하면서 이메일 값이 바뀌었음

			// when & then
			assertThatThrownBy(() -> authService.login("user@example.com", "Password1!"))
					.isInstanceOf(BadCredentialsException.class);
		}
	}

	@Nested
	@DisplayName("이메일 인증")
	class VerifyEmail {

		@Test
		@DisplayName("유효한 토큰으로 이메일 인증 시 유저 상태가 ACTIVE로 변경되고 토큰이 사용 처리된다")
		void verifyEmail_success() {
			// given
			authService.signup(createSignupRequest("verify@example.com"));
			User user = userRepository.findByEmail("verify@example.com").orElseThrow();
			VerificationToken token = tokenRepository.findAll()
													 .stream()
													 .filter(t -> t.getUser().getId().equals(user.getId()))
													 .findFirst()
													 .orElseThrow();

			// when
			authService.verifyEmail(token.getToken());

			// then
			User updatedUser = userRepository.findByEmail("verify@example.com").orElseThrow();
			assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

			VerificationToken usedToken = tokenRepository.findByToken(token.getToken()).orElseThrow();
			assertThat(usedToken.getUsedAt()).isNotNull();
		}

		@Test
		@DisplayName("존재하지 않는 토큰으로 인증 시 VERIFICATION_TOKEN_NOT_FOUND 예외가 발생한다")
		void verifyEmail_tokenNotFound_throwsException() {
			assertThatThrownBy(() -> authService.verifyEmail("non-existent-token"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND));
		}

		@Test
		@DisplayName("이미 사용된 토큰으로 인증 시 VERIFICATION_TOKEN_ALREADY_USED 예외가 발생한다")
		void verifyEmail_alreadyUsedToken_throwsException() {
			// given
			authService.signup(createSignupRequest("used@example.com"));
			User user = userRepository.findByEmail("used@example.com").orElseThrow();
			VerificationToken token = tokenRepository.findAll()
													 .stream()
													 .filter(t -> t.getUser().getId().equals(user.getId()))
													 .findFirst()
													 .orElseThrow();

			authService.verifyEmail(token.getToken()); // 첫 번째 사용

			// when & then
			assertThatThrownBy(() -> authService.verifyEmail(token.getToken()))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
							.isEqualTo(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED));
		}

		@Test
		@DisplayName("만료된 토큰으로 인증 시 VERIFICATION_TOKEN_EXPIRED 예외가 발생한다")
		void verifyEmail_expiredToken_throwsException() throws Exception {
			// given
			authService.signup(createSignupRequest("expired@example.com"));
			User user = userRepository.findByEmail("expired@example.com").orElseThrow();
			VerificationToken token = tokenRepository.findAll()
													 .stream()
													 .filter(t -> t.getUser().getId().equals(user.getId()))
													 .findFirst()
													 .orElseThrow();
			expireToken(token); // 강제로 만료시키기

			// when & then
			assertThatThrownBy(() -> authService.verifyEmail(token.getToken()))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
							.isEqualTo(ErrorCode.VERIFICATION_TOKEN_EXPIRED));
		}
	}

	@Nested
	@DisplayName("계정 인증용 이메일 재전송")
	class ResendVerificationEmail {

		@Test
		@DisplayName("만료된 토큰으로 재전송 요청 시 기존 토큰 삭제 후 새 토큰 발급 및 이메일 발송")
		void resend_success() throws Exception {
			// given
			authService.signup(createSignupRequest("resend@example.com"));
			User user = userRepository.findByEmail("resend@example.com").orElseThrow();
			VerificationToken oldToken = tokenRepository.findAll()
														.stream()
														.filter(t -> t.getUser().getId().equals(user.getId()))
														.findFirst()
														.orElseThrow();
			String oldTokenValue = oldToken.getToken();
			expireToken(oldToken);

			// when
			authService.resendVerificationEmail(oldTokenValue);

			// then
			// 기존 토큰은 삭제됨
			assertThat(tokenRepository.findByToken(oldTokenValue)).isEmpty();

			// 이메일 발송: signup(1회) + resend(1회) = 2회
			verify(emailService, times(2)).sendTemplateEmail(eq("resend@example.com"), any(VerifyEmailContext.class));
		}

		@Test
		@DisplayName("존재하지 않는 토큰으로 재전송 요청 시 VERIFICATION_TOKEN_NOT_FOUND 예외")
		void resend_tokenNotFound_throwsException() {
			assertThatThrownBy(() -> authService.resendVerificationEmail("invalid-token"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND));
		}

		@Test
		@DisplayName("이미 사용된 토큰으로 재전송 요청 시 VERIFICATION_TOKEN_ALREADY_USED 예외")
		void resend_alreadyUsedToken_throwsException() {
			// given
			authService.signup(createSignupRequest("resendused@example.com"));
			User user = userRepository.findByEmail("resendused@example.com").orElseThrow();
			VerificationToken token = tokenRepository.findAll()
													 .stream()
													 .filter(t -> t.getUser().getId().equals(user.getId()))
													 .findFirst()
													 .orElseThrow();

			authService.verifyEmail(token.getToken()); // 토큰 사용 처리

			// when & then
			assertThatThrownBy(() -> authService.resendVerificationEmail(token.getToken()))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED));
		}
	}


	@Nested
	@DisplayName("비밀번호 재설정 이메일 요청")
	class RequestResetPassword {

		@Test
		@DisplayName("ACTIVE 상태의 사용자가 비밀번호 재설정 요청 시 토큰이 생성되고 이메일이 발송된다")
		void requestReset_activeUser_success() {
			// given
			saveActiveUser("reset@example.com");

			// when
			authService.requestResetPassword("reset@example.com");

			// then
			verify(emailService, times(1)).sendTemplateEmail(eq("reset@example.com"), any(PasswordResetEmailContext.class));
		}

		@Test
		@DisplayName("PENDING 상태의 사용자도 비밀번호 재설정 요청이 가능하다")
		void requestReset_pendingUser_success() {
			// given
			authService.signup(createSignupRequest("pendingReset@example.com"));
			reset(emailService); // signup에서 발송된 이메일 카운트 초기화

			// when
			authService.requestResetPassword("pendingReset@example.com");

			// then
			verify(emailService, times(1)).sendTemplateEmail(eq("pendingReset@example.com"), any(PasswordResetEmailContext.class));
		}

		@Test
		@DisplayName("존재하지 않는 이메일로 요청 시 USER_NOT_FOUND 예외가 발생한다")
		void requestReset_userNotFound_throwsException() {
			assertThatThrownBy(() -> authService.requestResetPassword("nobody@example.com"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
		}

		@Test
		@DisplayName("BANNED/DELETED 상태의 사용자가 요청 시 LOCKED 예외가 발생한다")
		void requestReset_lockedUser_throwsException() {
			// given
			User user = saveActiveUser("banned@example.com");
			user.ban();

			// when & then
			assertThatThrownBy(() -> authService.requestResetPassword("banned@example.com"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.LOCKED));
		}
	}


	@Nested
	@DisplayName("비밀번호 재설정")
	class ResetPassword {

		@Test
		@DisplayName("유효한 토큰으로 비밀번호 재설정 시 비밀번호가 변경되고 토큰이 사용 처리된다")
		void resetPassword_success() {
			// given
			User user = saveActiveUser("pwreset@example.com");
			VerificationToken token = tokenRepository.save(
					VerificationToken.createPasswordVerificationToken(user)
			);

			// when
			authService.resetPassword(token.getToken(), "NewPassword1!");

			// then
			User updatedUser = userRepository.findByEmail("pwreset@example.com").orElseThrow();
			assertThat(passwordEncoder.matches("NewPassword!", updatedUser.getPassword()));

			VerificationToken usedToken = tokenRepository.findByToken(token.getToken()).orElseThrow();
			assertThat(usedToken.getUsedAt()).isNotNull();
		}

		@Test
		@DisplayName("존재하지 않는 토큰으로 재설정 시 VERIFICATION_TOKEN_NOT_FOUND 예외 발생")
		void resetPassword_tokenNotFound() {
			assertThatThrownBy(() -> authService.resetPassword("invalid", "newPwd"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND));
		}

		@Test
		@DisplayName("이미 사용된 토큰으로 재설정 시 VERIFICATION_TOKEN_ALREADY_USED 예외 발생")
		void resetPassword_alreadyUsed() {
			// given
			User user = saveActiveUser("pwused@example.com");
			VerificationToken token = tokenRepository.save(
					VerificationToken.createPasswordVerificationToken(user)
			);

			authService.resetPassword(token.getToken(), "NewPassword1!");

			// when & then

			assertThatThrownBy(() -> authService.resetPassword(token.getToken(), "AnotherPwd1!"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED));
		}

		@Test
		@DisplayName("만료된 토큰으로 재설정 시 VERIFICATION_TOKEN_EXPIRED 예외 발생")
		void resetPassword_expired() throws Exception {
			User user = saveActiveUser("pwexpired@example.com");
			VerificationToken token = tokenRepository.save(
					VerificationToken.createPasswordVerificationToken(user)
			);
			expireToken(token);

			assertThatThrownBy(() -> authService.resetPassword(token.getToken(), "NewPwd1!"))
					.isInstanceOf(CustomException.class)
					.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_EXPIRED));
		}
	}
}