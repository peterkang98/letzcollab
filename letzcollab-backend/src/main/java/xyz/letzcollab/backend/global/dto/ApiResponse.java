package xyz.letzcollab.backend.global.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.letzcollab.backend.global.exception.ErrorCode;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
	private final boolean success;
	private final String message;
	private final T data;
	private final String errorCode;

	@Builder.Default
	private final Long timestamp = System.currentTimeMillis();

	public static <T> ApiResponse<T> success(T data, String message) {
		return ApiResponse.<T>builder()
						  .success(true)
						  .data(data)
						  .message(message)
						  .build();
	}

	public static <T> ApiResponse<T> success(T data) {
		return ApiResponse.<T>builder()
						  .success(true)
						  .data(data)
						  .message("요청이 성공적으로 처리되었습니다.")
						  .build();
	}

	public static <T> ApiResponse<T> success(String message) {
		return ApiResponse.<T>builder()
						  .success(true)
						  .message(message)
						  .build();
	}

	public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
		return ApiResponse.<T>builder()
						  .success(false)
						  .message(errorCode.getMessage())
						  .errorCode(errorCode.getCode())
						  .build();
	}

	public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
		return ApiResponse.<T>builder()
						  .success(false)
						  .message(message)
						  .errorCode(errorCode.getCode())
						  .build();
	}

	public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
		return ApiResponse.<T>builder()
						  .success(false)
						  .message(errorCode.getMessage())
						  .errorCode(errorCode.getCode())
						  .data(data)
						  .build();
	}
}
