package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import xyz.letzcollab.backend.dto.notification.NotificationResponse;
import xyz.letzcollab.backend.dto.notification.NotificationSseEvent;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.global.sse.SseEmitterService;
import xyz.letzcollab.backend.service.NotificationService;

@Tag(name = "06. Notification", description = "사용자 알림 조회 및 읽음 처리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/notifications")
@Slf4j
public class NotificationController {

	private final NotificationService notificationService;
	private final SseEmitterService sseEmitterService;

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

	/**
	 * SSE 알림 구독
	 */
	@Operation(summary = "SSE 알림 구독", description = "Server-Sent Events로 실시간 알림을 구독합니다.")
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
		SseEmitter emitter = sseEmitterService.subscribe(userDetails.getPublicId());

		// 구독 즉시 현재 미확인 개수 전송 (초기값)
		notificationService.sendInitialUnread(userDetails.getPublicId());

		return emitter;
	}

	//  sse 알림 구독 테스트용
//	@GetMapping(value = "/subscribe-test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//	public SseEmitter subscribeTest(@AuthenticationPrincipal CustomUserDetails userDetails) {
//		SseEmitter emitter = sseEmitterService.subscribe(userDetails.getPublicId());
//
//		// 비동기 스레드 풀에 초기값 전송 업무 맏기고 성공 시 바로 연결 끊음
//		notificationService.sendInitialUnreadCountTest(userDetails.getPublicId());
//
//		return emitter;
//	}
}