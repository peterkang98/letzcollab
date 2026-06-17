package xyz.letzcollab.backend.global.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import xyz.letzcollab.backend.dto.notification.NotificationResponse;
import xyz.letzcollab.backend.dto.notification.NotificationSseEvent;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.global.sse.SseEmitterService;
import xyz.letzcollab.backend.service.NotifCntCacheService;
import xyz.letzcollab.backend.service.NotificationService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

	private final NotificationService notificationService;
	private final SseEmitterService sseEmitterService;
	private final NotifCntCacheService cacheService;

	@Async("notificationExecutor")
	@TransactionalEventListener
	public void handle(NotificationEvent event) {
		try {
			Notification notification = notificationService.create(event);
			UUID userPublicId = notification.getRecipient().getPublicId();

			cacheService.increment(userPublicId);

			if (sseEmitterService.isSubscribed(userPublicId)) {
				NotificationResponse res = NotificationResponse.from(notification);
				long unreadCount = notificationService.getUnreadCount(userPublicId);
				sseEmitterService.send(userPublicId, "notification", new NotificationSseEvent(res, unreadCount));
			}
		} catch (Exception e) {
			log.error("알림 생성 실패 - type={}, recipientId={}, referenceId={}",
					event.type(), event.recipientId(), event.referenceId(), e);
		}
	}
}
