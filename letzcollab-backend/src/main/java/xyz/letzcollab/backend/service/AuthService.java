package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.auth.LoginResponse;
import xyz.letzcollab.backend.dto.auth.SignupRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.VerificationToken;
import xyz.letzcollab.backend.entity.vo.TokenType;
import xyz.letzcollab.backend.global.email.context.PasswordResetEmailContext;
import xyz.letzcollab.backend.global.email.context.VerifyEmailContext;
import xyz.letzcollab.backend.global.event.dto.EmailEvent;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.global.ratelimit.AuthRateLimiter;
import xyz.letzcollab.backend.global.security.jwt.JwtTokenProvider;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.VerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
	private final ApplicationEventPublisher eventPublisher;

	private final UserRepository userRepository;
	private final VerificationTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthRateLimiter authRateLimiter;

	@Value("${frontend.base-url}")
	private String frontendURL;

	public void signup(SignupRequest req) {
		if (userRepository.existsByEmail(req.email())) {
			log.warn("회원가입 실패 - 이메일 중복: {}", req.email());
			throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = User.createPendingUser(
				req.name(),
				req.email(),
				passwordEncoder.encode(req.password()),
				req.phoneNumber()
		);
		userRepository.save(user);
		log.info("회원가입 완료 - email: {}", req.email());

		VerificationToken token = VerificationToken.createEmailVerificationToken(user);
		tokenRepository.save(token);

		sendVerificationEmail(req.name(), token.getToken().toString(), req.email());
		log.info("이메일 인증 메일 발송 - email: {}", req.email());
	}

	@Transactional(readOnly = true)
	public LoginResponse login(String email, String password) {
		log.debug("로그인 시도 - email: {}", email);
		try {
			UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);

			Authentication authentication = authenticationManager.authenticate(authenticationToken);

			String token = jwtTokenProvider.createToken(authentication);
			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

			log.info("로그인 성공 - email: {}", email);
			return new LoginResponse(token, userDetails.getName(), userDetails.getEmail());

		} catch (BadCredentialsException e) {
			log.warn("로그인 실패 - 이메일 또는 비밀번호 불일치: {}", email);
			throw e;
		} catch (DisabledException e) {
			log.warn("로그인 실패 - 이메일 미인증: {}", email);
			throw e;
		} catch (LockedException e) {
			log.warn("로그인 실패 - 계정 잠김/탈퇴: {}", email);
			throw e;
		}
	}

	public void verifyEmail(UUID token) {
		log.debug("이메일 인증 시도 - token: {}", token);

		VerificationToken foundToken = getVerificationToken(token);
		foundToken.getUser().verifyEmail();
		foundToken.use();

		log.info("이메일 인증 완료 - email: {}", foundToken.getUser().getEmail());
	}

	public void resendVerificationEmail(UUID expiredToken) {
		log.debug("인증 메일 재발송 요청 - token: {}", expiredToken);

		VerificationToken foundExpiredToken = tokenRepository.findByToken(expiredToken)
															 .orElseThrow(() -> {
																 log.warn("인증 메일 재발송 실패 - 토큰 없음: {}", expiredToken);
																 return new CustomException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND);
															 });

		verifyTokenExpirationAndUsage(expiredToken, foundExpiredToken);

		User foundUser = foundExpiredToken.getUser();
		VerificationToken newToken = VerificationToken.createEmailVerificationToken(foundUser);

		tokenRepository.delete(foundExpiredToken);
		tokenRepository.save(newToken);

		sendVerificationEmail(foundUser.getName(), newToken.getToken().toString(), foundUser.getEmail());
		log.info("인증 메일 재발송 완료 - email: {}", foundUser.getEmail());
	}

	public void requestResetPassword(String email) {
		log.debug("비밀번호 재설정 요청 - email: {}", email);

		User foundUser = userRepository.findByEmail(email)
									   .orElseThrow(() -> {
										   log.warn("비밀번호 재설정 실패 - 존재하지 않는 이메일: {}", email);
										   return new CustomException(ErrorCode.USER_NOT_FOUND);
									   });

		if (!foundUser.getStatus().canResetPassword()) {
			log.warn("비밀번호 재설정 실패 - 정지 또는 탈퇴 계정: {}", email);
			throw new CustomException(ErrorCode.LOCKED);
		}

		authRateLimiter.rateLimitResetPwdReq(email);

		VerificationToken passwordResetToken = VerificationToken.createPasswordVerificationToken(foundUser);
		tokenRepository.save(passwordResetToken);

		PasswordResetEmailContext emailContext = new PasswordResetEmailContext(
				foundUser.getName(), passwordResetToken.getToken().toString(), frontendURL
		);

		eventPublisher.publishEvent(new EmailEvent(email, emailContext));
		log.info("비밀번호 재설정 메일 발송 완료 - email: {}", email);
	}

	public void resetPassword(UUID token, String newPassword) {
		log.debug("비밀번호 재설정 시도 - token: {}", token);

		VerificationToken foundToken = getVerificationToken(token);
		foundToken.getUser().resetPassword(passwordEncoder.encode(newPassword));
		foundToken.use();

		log.info("비밀번호 재설정 완료 - email: {}", foundToken.getUser().getEmail());
	}


	// 헬퍼
	private VerificationToken getVerificationToken(UUID token) {
		VerificationToken foundToken = tokenRepository.findByToken(token)
													  .orElseThrow(() -> {
														  log.warn("토큰 조회 실패 - 존재하지 않는 토큰: {}", token);
														  return new CustomException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND);
													  });

		if (foundToken.getUsedAt() != null) {
			log.warn("토큰 검증 실패 - 이미 사용된 토큰: {}", token);
			throw new CustomException(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED);
		}
		if (foundToken.isExpired()) {
			log.warn("토큰 검증 실패 - 만료된 토큰: {}", token);
			throw new CustomException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
		}
		return foundToken;
	}

	private void sendVerificationEmail(String name, String token, String email) {
		VerifyEmailContext emailContext = new VerifyEmailContext(name, token, frontendURL);
		eventPublisher.publishEvent(new EmailEvent(email, emailContext));
	}

	private void verifyTokenExpirationAndUsage(UUID expiredToken, VerificationToken foundExpiredToken) {
		if (foundExpiredToken.getUsedAt() != null) {
			log.warn("인증 메일 재발송 실패 - 이미 사용된 토큰: {}", expiredToken);
			throw new CustomException(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED);
		} else if (foundExpiredToken.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(4))) {
			throw new CustomException(ErrorCode.VERIFICATION_TOKEN_NOT_EXPIRED);
		}
	}
}
