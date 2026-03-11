package xyz.letzcollab.backend.global.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.service.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

	private final NotificationService notificationService;

	@Async("notificationExecutor")
	@TransactionalEventListener
	public void handle(NotificationEvent event) {
		try {
			notificationService.create(event);
		} catch (Exception e) {
			log.error("알림 생성 실패 - type={}, recipientId={}, referenceId={}",
					event.type(), event.recipientId(), event.referenceId(), e);
		}
	}
}
