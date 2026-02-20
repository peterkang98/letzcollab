package xyz.letzcollab.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.exception.ErrorCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthErrorHandler {
	private final ObjectMapper objectMapper;

	public void handle(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Void> apiResponse = ApiResponse.fail(errorCode);

		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
	}
}
