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

	// --- Auth (A) ---
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증되지 않은 사용자입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),

	// --- User (U) ---
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 가입된 이메일입니다."),
	USER_ALREADY_LOGGED_IN(HttpStatus.BAD_REQUEST, "U003", "이미 로그인된 상태입니다."),

	// --- Email (E) ---
	EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "이메일 발송에 실패했습니다."),
	VERIFICATION_CODE_EXPIRED(HttpStatus.GONE, "E002", "인증 코드가 만료되었습니다."),
	VERIFICATION_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "E003", "해당 검증 토큰을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
