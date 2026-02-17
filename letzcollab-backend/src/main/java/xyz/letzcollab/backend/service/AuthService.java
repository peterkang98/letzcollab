package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.auth.LoginResponse;
import xyz.letzcollab.backend.dto.auth.SignupRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.VerificationToken;
import xyz.letzcollab.backend.global.email.EmailService;
import xyz.letzcollab.backend.global.email.context.PasswordResetEmailContext;
import xyz.letzcollab.backend.global.email.context.VerifyEmailContext;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.global.security.jwt.JwtTokenProvider;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.VerificationTokenRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
	private final UserRepository userRepository;
	private final VerificationTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;

	@Value("${frontend.base-url}")
	private String frontendURL;

	public void signup(SignupRequest req) {
		if (userRepository.existsByEmail(req.email())) {
			throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = User.createPendingUser(
				req.name(),
				req.email(),
				passwordEncoder.encode(req.password()),
				req.phoneNumber()
		);
		userRepository.save(user);

		VerificationToken token = VerificationToken.createEmailVerificationToken(user);
		tokenRepository.save(token);

		VerifyEmailContext emailContext = new VerifyEmailContext(req.name(), token.getToken(), frontendURL);
		emailService.sendTemplateEmail(req.email(), emailContext);
	}

	@Transactional(readOnly = true)
	public LoginResponse login(String email, String password) {
		try {
			UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);

			Authentication authentication = authenticationManager.authenticate(authenticationToken);

			String token = jwtTokenProvider.createToken(authentication);
			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

			return new LoginResponse(token, userDetails.getName(), userDetails.getEmail());
		} catch (BadCredentialsException e) {
			log.warn("로그인 실패: 이메일 또는 비밀번호 불일치 [Email: {}]", email);
			throw e;
		} catch (DisabledException e) {
			log.warn("로그인 실패: 이메일 미인증 [Email: {}]", email);
			throw e;
		} catch (LockedException e) {
			log.warn("로그인 실패: 계정 잠김/탈퇴 [Email: {}]", email);
			throw e;
		}
	}

	public void verifyEmail(String token) {
		VerificationToken foundToken = getVerificationToken(token);

		foundToken.getUser().verifyEmail();
		foundToken.use();
	}

	public void requestResetPassword(String email) {
		User foundUser = userRepository.findByEmail(email)
									   .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (!foundUser.getStatus().canResetPassword()) {
			throw new CustomException(ErrorCode.LOCKED);
		}

		VerificationToken passwordResetToken = VerificationToken.createPasswordVerificationToken(foundUser);
		tokenRepository.save(passwordResetToken);

		PasswordResetEmailContext emailContext = new PasswordResetEmailContext(
				foundUser.getName(), passwordResetToken.getToken(), frontendURL
		);
		emailService.sendTemplateEmail(email, emailContext);
	}

	public void resetPassword(String token, String newPassword) {
		VerificationToken foundToken = getVerificationToken(token);

		foundToken.getUser().resetPassword(passwordEncoder.encode(newPassword));
		foundToken.use();
	}

	private VerificationToken getVerificationToken(String token) {
		VerificationToken foundToken = tokenRepository.findByToken(token)
													  .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND));

		if (foundToken.getUsedAt() != null) {
			throw new CustomException(ErrorCode.VERIFICATION_TOKEN_ALREADY_USED);
		}
		if (foundToken.isExpired()) {
			throw new CustomException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
		}
		return foundToken;
	}
}
