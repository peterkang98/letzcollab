package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.user.UserResponse;
import xyz.letzcollab.backend.dto.user.UserUpdateRequest;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.UserService;

@Tag(name = "02. User", description = "현재 로그인한 사용자의 정보 조회/수정/탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
	private final UserService userService;

	/**
	 * 내 정보 조회
	 */
	@Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 상세 정보를 조회합니다.")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
		UserResponse response = userService.getMyInfo(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 내 정보 수정 (이메일 제외)
	 */
	@Operation(summary = "내 정보 수정", description = "이메일을 제외한 사용자 기본 정보(이름, 전화번호)를 수정합니다.")
	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody UserUpdateRequest request) {

		UserResponse response = userService.updateMyInfo(userDetails.getPublicId(), request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 회원 탈퇴
	 */
	@Operation(summary = "회원 탈퇴", description = "계정 상태를 탈퇴로 변경 (Soft Delete)")
	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.withdraw(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success("탈퇴 요청이 정상 처리되었습니다"));
	}
}
