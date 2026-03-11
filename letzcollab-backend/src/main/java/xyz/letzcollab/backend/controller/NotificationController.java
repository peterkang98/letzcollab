package xyz.letzcollab.backend.controller;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	/** 내 알림 목록 조회 (페이지네이션) */
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
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long notificationId
	) {
		notificationService.markAsRead(userDetails.getPublicId(), notificationId);
		return ResponseEntity.ok(ApiResponse.success("알림 읽음 처리 완료"));
	}

	/** 전체 읽음 처리 */
	@PatchMapping("/read-all")
	public ResponseEntity<ApiResponse<Void>> markAllAsRead(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		notificationService.markAllAsRead(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success("알림 전체 읽음 처리 완료"));
	}

	/** 읽지 않은 알림 개수 조회 */
	@GetMapping("/unread-count")
	public ResponseEntity<ApiResponse<Long>> getUnreadCount(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		long count = notificationService.getUnreadCount(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success(count));
	}
}