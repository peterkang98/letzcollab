package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.notification.NotificationResponse;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.NotificationService;

@Tag(name = "06. Notification", description = "사용자 알림 조회 및 읽음 처리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	/** 내 알림 목록 조회 (페이지네이션) */
	@Operation(summary = "내 알림 목록 페이징 조회", description = "나에게 온 알림 목록을 최신순으로 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Pageable pageable
	) {
		Page<NotificationResponse> response = notificationService.getMyNotifications(
				userDetails.getPublicId(), pageable
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/** 단건 읽음 처리 */
	@Operation(summary = "단건 알림 읽음 처리", description = "특정 알림 1건을 읽음 처리합니다.")
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long notificationId
	) {
		notificationService.markAsRead(userDetails.getPublicId(), notificationId);
		return ResponseEntity.ok(ApiResponse.success("알림 읽음 처리 완료"));
	}

	/** 전체 읽음 처리 */
	@Operation(summary = "전체 알림 읽음 처리", description = "나에게 온 모든 미확인 알림을 일괄 읽음 처리합니다.")
	@PatchMapping("/read-all")
	public ResponseEntity<ApiResponse<Void>> markAllAsRead(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		notificationService.markAllAsRead(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success("알림 전체 읽음 처리 완료"));
	}

	/** 읽지 않은 알림 개수 조회 */
	@Operation(summary = "읽지 않은 알림 개수 조회", description = "읽지 않은 알림의 총 개수를 반환합니다.")
	@GetMapping("/unread-count")
	public ResponseEntity<ApiResponse<Long>> getUnreadCount(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		long count = notificationService.getUnreadCount(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success(count));
	}
}