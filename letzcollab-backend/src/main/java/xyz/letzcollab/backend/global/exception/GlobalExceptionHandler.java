package xyz.letzcollab.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.exception.dto.ValidationError;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("커스텀 예외 발생: {} - {}", errorCode.getCode(), e.getMessage());

		return ResponseEntity.status(errorCode.getStatus())
							 .body(ApiResponse.fail(errorCode));
	}

	// -- Spring Security 관련 예외 --
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
							 .body(ApiResponse.fail(ErrorCode.BAD_CREDENTIALS));
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
							 .body(ApiResponse.fail(ErrorCode.DISABLED));
	}

	@ExceptionHandler(LockedException.class)
	public ResponseEntity<ApiResponse<Void>> handleLockedException(LockedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
							 .body(ApiResponse.fail(ErrorCode.LOCKED));
	}
	// ----

	// @Valid 검증 예외 + 객체 바인딩 예외 (@RequestBody, @ModelAttribute 공통 처리)
	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiResponse<List<ValidationError>>> handleBindingException(BindException e) {
		List<ValidationError> errMsgList = e.getBindingResult()
											.getFieldErrors()
											.stream()
											.map(ValidationError::of)
											.toList();

		log.warn("검증 또는 바인딩 실패: {}", errMsgList);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, errMsgList));
	}

	// JSON 문법 자체의 오류 (Body 읽기 실패)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.warn("JSON 파싱 실패 또는 잘못된 형식의 요청: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(ApiResponse.fail(ErrorCode.INVALID_JSON_FORMAT));
	}

	// 단일 파라미터 타입 불일치 (@PathVariable, @RequestParam)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
		log.warn("메소드 인자 타입 불일치: {} - {}", e.getName(), e.getValue());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(ApiResponse.fail(ErrorCode.INVALID_TYPE_VALUE));
	}

	// 그 외 예상치 못한 모든 예외
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("처리되지 않은 예외 발생: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							 .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
