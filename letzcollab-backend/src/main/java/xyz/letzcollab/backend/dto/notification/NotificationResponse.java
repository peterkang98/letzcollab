package xyz.letzcollab.backend.dto.notification;

import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
		Long notificationId,
		NotificationType type,
		ReferenceType referenceType,
		UUID referenceId,
		UUID parentReferenceId,
		String message,
		boolean isRead,
		LocalDateTime createdAt
) {
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getReferenceType(),
				notification.getReferenceId(),
				notification.getParentReferenceId(),
				notification.getMessage(),
				notification.isRead(),
				notification.getCreatedAt()
		);
	}
}