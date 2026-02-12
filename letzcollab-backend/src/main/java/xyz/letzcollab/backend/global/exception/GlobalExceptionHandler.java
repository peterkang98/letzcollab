package xyz.letzcollab.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.exception.dto.ValidationError;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("커스텀 예외 발생: {} - {}", errorCode, e.getMessage());

		return ResponseEntity.status(errorCode.getStatus())
							 .body(ApiResponse.fail(errorCode));
	}

	// @Valid 예외 처리
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<List<ValidationError>>> handleValidationException(MethodArgumentNotValidException e) {
		List<ValidationError> errMsgList = e.getBindingResult()
											.getFieldErrors()
											.stream()
											.map(ValidationError::of)
											.toList();

		log.warn("입력값 검증 실패: {}", errMsgList);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, errMsgList));
	}

	// 그 외 예상치 못한 모든 예외
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("처리되지 않은 예외 발생: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							 .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
