package xyz.letzcollab.backend.global.event.dto;

import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;

import java.util.UUID;

public record NotificationEvent(
		Long recipientId,
		NotificationType type,
		ReferenceType referenceType,
		UUID referenceId,
		UUID parentReferenceId,
		String message
) {
}
