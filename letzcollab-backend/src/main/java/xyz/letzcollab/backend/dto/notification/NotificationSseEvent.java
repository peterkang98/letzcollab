package xyz.letzcollab.backend.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SSE로 전송되는 알림 이벤트")
public record NotificationSseEvent(
	@Schema(description = "새로 생성된 알림 정보")
	NotificationResponse notification,

	@Schema(description = "현재 미확인 알림 개수")
	long unreadCount
) {}
