package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.auth.*;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.AuthService;

import java.net.URI;

@Tag(name = "01. Auth", description = "회원가입, 로그인 및 이메일 인증 API")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {
	private final AuthService authService;
	private final long jwtAccessValidityInMs;
	private final long jwtRefreshValidityInDays;

	public AuthController(
			AuthService authService,
			@Value("${jwt.access-validity-in-ms}") long jwtAccessValidityInMs,
			@Value("${jwt.refresh-validity-in-days}") long jwtRefreshValidityInDays
	) {
		this.authService = authService;
		this.jwtAccessValidityInMs = jwtAccessValidityInMs;
		this.jwtRefreshValidityInDays = jwtRefreshValidityInDays;
	}

	@Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. 30분 이내에 이메일 인증을 완료해야 로그인이 가능합니다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest req) {
		authService.signup(req);
		return ResponseEntity.created(URI.create("/v1/users/me"))
							 .body(ApiResponse.success("회원가입 성공! 로그인은 이메일 인증 후 가능합니다. 30분 이내에 이메일 인증을 완료해주세요"));
	}

	@Operation(
			summary = "로그인",
			description = "이메일과 비밀번호로 로그인합니다.<br>웹 프론트엔드는 `X-Client-Type: web` 헤더 전송 시 HttpOnly 쿠키가 자동 세팅됩니다."
	)
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(
			@Valid @RequestBody LoginRequest req,
			@RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
			HttpServletResponse httpServletResponse
	) {
		LoginResponse loginResponse = authService.login(req.email(), req.password());

		// 1. 웹 브라우저인 경우 JWT를 쿠키로만 전송
		if ("web".equalsIgnoreCase(clientType)) {
			setCookies(httpServletResponse, loginResponse.accessToken(), loginResponse.refreshToken());
			return ResponseEntity.ok(ApiResponse.success(loginResponse.withoutToken(), "로그인 성공!"));
		}

		// 2. 모바일 앱인 경우 JWT를 바디로만 전송
		return ResponseEntity.ok(ApiResponse.success(loginResponse, "로그인 성공!"));
	}

	@Operation(
			summary = "JWT 재발급",
			description = "JWT와 갱신 토큰을 새로 발급합니다.<br>웹 프론트엔드는 `X-Client-Type: web` 헤더 전송 시 HttpOnly 쿠키가 자동 세팅됩니다."
	)
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
			@RequestBody(required = false) RefreshRequest req,
			@CookieValue(name = "refreshToken", required = false) String refreshToken,
			@RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
			HttpServletResponse httpServletResponse
	) {
		RefreshResponse res;

		if ("web".equalsIgnoreCase(clientType)) {
			res = authService.refresh(refreshToken);
			setCookies(httpServletResponse, res.accessToken(), res.refreshToken());
			return ResponseEntity.ok(ApiResponse.success(res.withoutToken(), "JWT 재발급 성공!"));
		}

		res = authService.refresh(req.refreshToken());
		return ResponseEntity.ok(ApiResponse.success(res, "JWT 재발급 성공!"));
	}

	@Operation(summary = "로그아웃", description = "웹 브라우저인 경우 JWT/갱신 토큰 쿠키를 만료시키고, 모바일인 경우 갱신 토큰을 redis에서 제거하여 로그아웃 처리합니다.")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody(required = false) LogoutRequest req,
			@CookieValue(name = "refreshToken", required = false) String refreshToken,
			@RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
			HttpServletResponse httpServletResponse
	) {
		String userEmail = userDetails.getEmail();

		if ("web".equalsIgnoreCase(clientType)) {
			authService.logout(refreshToken, userEmail);
			removeCookies(httpServletResponse);
		} else {
			authService.logout(req.refreshToken(), userEmail);
		}

		return ResponseEntity.ok(ApiResponse.success("로그아웃 성공!"));
	}

	@Operation(summary = "이메일 인증", description = "회원가입 시 발송된 이메일의 토큰을 검증하여 계정을 활성화합니다.")
	@PostMapping("/verify-email")
	public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody EmailVerificationRequest req) {
		authService.verifyEmail(req.token());
		return ResponseEntity.ok(ApiResponse.success("이메일 인증 성공!"));
	}

	@Operation(summary = "회원가입 이메일 인증용 링크 재발송", description = "만료된 이메일 인증 토큰을 전달받아 새로운 인증 메일을 재발송합니다.")
	@PostMapping("/verify-email/resend")
	public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
			@Valid @RequestBody ResendEmailVerificationRequest req
	) {
		authService.resendVerificationEmail(req.expiredToken());
		return ResponseEntity.ok(ApiResponse.success("이메일 재전송 완료! 30분 이내에 이메일 인증을 완료해주세요."));
	}

	@Operation(summary = "비밀번호 초기화 이메일 요청", description = "가입된 이메일로 비밀번호 재설정 링크(토큰)를 발송합니다.")
	@PostMapping("/password/reset-request")
	public ResponseEntity<ApiResponse<Void>> requestResetPassword(@Valid @RequestBody PasswordResetEmailRequest req) {
		authService.requestResetPassword(req.email());
		return ResponseEntity.ok(ApiResponse.success("비밀번호 초기화 이메일 전송 완료! 30분 이내에 초기화 부탁드립니다."));
	}

	@Operation(summary = "비밀번호 초기화 수행", description = "이메일로 받은 토큰을 검증하고 새로운 비밀번호로 변경합니다.")
	@PostMapping("/password/reset")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
		authService.resetPassword(req.token(), req.newPassword());
		return ResponseEntity.ok(ApiResponse.success("비밀번호 초기화 성공!"));
	}


	// 헬퍼
	private ResponseCookie getTokenCookie(boolean isAccessToken, String cookieVal, long maxAge) {
		String cookieName = isAccessToken ? "accessToken" : "refreshToken";

		return ResponseCookie.from(cookieName, cookieVal)
							 .httpOnly(true)    // XSS 방지(JS에서 접근 불가)
							 .path("/")
							 .maxAge(maxAge)
							 .sameSite("Lax")    // CSRF 방지 (같은 사이트 + 외부에서 링크 클릭으로 이동만 허용)
							 .build();
	}

	private void setCookies(HttpServletResponse httpServletResponse, String accessToken, String refreshToken) {
		ResponseCookie accessTokenCookie = getTokenCookie(true, accessToken, jwtAccessValidityInMs/1000);
		ResponseCookie refreshTokenCookie = getTokenCookie(false, refreshToken, jwtRefreshValidityInDays * 86400);
		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
	}

	private void removeCookies(HttpServletResponse httpServletResponse) {
		ResponseCookie accessTokenCookie = getTokenCookie(true, "", 0);
		ResponseCookie refreshTokenCookie = getTokenCookie(false, "", 0);
		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
	}
}