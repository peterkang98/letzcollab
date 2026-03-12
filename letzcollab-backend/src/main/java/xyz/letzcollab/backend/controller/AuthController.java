package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.auth.*;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.service.AuthService;

import java.net.URI;

@Tag(name = "01. Auth", description = "회원가입, 로그인 및 이메일 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;

	@Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. 30분 이내에 이메일 인증을 완료해야 로그인이 가능합니다.")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest req) {
		authService.signup(req);
		return ResponseEntity.created(URI.create("/api/v1/users/me"))
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
			ResponseCookie cookie = ResponseCookie.from("accessToken", loginResponse.accessToken())
												  .httpOnly(true)	// XSS 방지(JS에서 접근 불가)
												  .secure(false)    // 로컬 테스트(http)용, 배포할 때 수정
												  .path("/")
												  .maxAge(60 * 30)
												  .sameSite("Lax")	// CSRF 방지
												  .build();

			httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

			return ResponseEntity.ok(ApiResponse.success(loginResponse.withoutToken(), "로그인 성공!"));
		}

		// 2. 모바일 앱인 경우 JWT를 바디로만 전송
		return ResponseEntity.ok(ApiResponse.success(loginResponse, "로그인 성공!"));
	}

	/**
	 *
	 * (웹 프론트 전용)
	 * 브라우저에서 JWT 쿠키를 없애고 싶을 때 호출
	 */
	@Operation(summary = "로그아웃", description = "웹 브라우저의 JWT 쿠키를 만료시켜 로그아웃 처리합니다.")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpServletResponse) {

		ResponseCookie cookie = ResponseCookie.from("accessToken", "")
											  .httpOnly(true)
											  .secure(false)	// 로컬 테스트(http)용, 배포할 때 수정
											  .path("/")
											  .maxAge(0)
											  .sameSite("Lax")
											  .build();

		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
}