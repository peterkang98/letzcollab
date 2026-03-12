package xyz.letzcollab.backend.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "알림 응답 정보 DTO")
public record NotificationResponse(
		@Schema(description = "알림 PK ID", example = "10")
		Long notificationId,

		@Schema(description = "알림 종류 (업무 할당, 상태 변경 등)", example = "TASK_ASSIGNED")
		NotificationType type,

		@Schema(description = "알림이 발생한 참조 도메인 (TASK, PROJECT)", example = "TASK")
		ReferenceType referenceType,

		@Schema(description = "참조 대상의 Public ID (Task UUID 등)", example = "123e4567-e89b-12d3-a456-426614174000")
		UUID referenceId,

		@Schema(description = "부모 참조 대상 ID (Project UUID 등)", example = "987e6543-e21b-12d3-a456-426614174111")
		UUID parentReferenceId,

		@Schema(description = "알림 메시지", example = "'DB 설계' 업무가 할당되었습니다.")
		String message,

		@Schema(description = "읽음 여부", example = "false")
		boolean isRead,

		@Schema(description = "알림 생성 일시")
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