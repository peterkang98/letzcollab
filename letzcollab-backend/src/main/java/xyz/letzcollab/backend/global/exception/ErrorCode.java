package xyz.letzcollab.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// --- Common (C) ---
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
	INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "데이터 타입이 올바르지 않습니다."),
	INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "C006", "잘못된 형식의 JSON 요청입니다."),

	// --- Auth (A) ---
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증되지 않은 사용자입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
	JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "만료된 JWT 토큰입니다."),
	JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 JWT 토큰입니다."),
	DISABLED(HttpStatus.FORBIDDEN, "A005", "이메일 인증을 먼저 완료해주세요."),
	LOCKED(HttpStatus.FORBIDDEN, "A006", "탈퇴했거나 차단된 계정입니다."),
	BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A007", "이메일 또는 비밀번호가 잘못되었습니다"),

	// --- User (U) ---
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 가입된 이메일입니다."),
	USER_ALREADY_LOGGED_IN(HttpStatus.BAD_REQUEST, "U003", "이미 로그인된 상태입니다."),

	// --- Email (E) ---
	EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "이메일 발송을 실패했습니다."),
	VERIFICATION_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "해당 인증 토큰을 찾을 수 없습니다."),
	VERIFICATION_TOKEN_EXPIRED(HttpStatus.GONE, "E003", "인증 토큰이 만료되었습니다."),
	VERIFICATION_TOKEN_ALREADY_USED(HttpStatus.GONE, "E004", "이미 사용된 인증 토큰입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
