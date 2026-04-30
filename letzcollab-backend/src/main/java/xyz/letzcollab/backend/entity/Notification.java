package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notifications", indexes = {
		@Index(name = "idx_notifications_recipient_created", columnList = "recipient_id, created_at DESC"),
		@Index(name = "idx_notifications_recipient_unread", columnList = "recipient_id, is_read"),
		@Index(name = "idx_notifications_recipient_reference_type", columnList = "recipient_id, reference_id, type")
})
public class Notification {

	@Id
	@GeneratedValue
	@Column(name = "notification_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false, updatable = false)
	private User recipient;

	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false, updatable = false)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type", length = 30, nullable = false, updatable = false)
	private ReferenceType referenceType;

	@Column(name = "reference_id", nullable = false, updatable = false)
	private UUID referenceId;

	@Column(name = "parent_reference_id", columnDefinition = "uuid", updatable = false)
	private UUID parentReferenceId;

	@Column(nullable = false, updatable = false)
	private String message;

	@Column(name = "is_read", nullable = false)
	private boolean isRead;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Builder(access = AccessLevel.PRIVATE)
	private Notification(User recipient, NotificationType type, ReferenceType referenceType,
						 UUID referenceId, UUID parentReferenceId, String message) {
		this.recipient = recipient;
		this.type = type;
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.parentReferenceId = parentReferenceId;
		this.message = message;
		this.isRead = false;
	}

	public static Notification create(User recipient, NotificationType type,
									  ReferenceType referenceType, UUID referenceId,
									  UUID parentReferenceId, String message) {
		return Notification.builder()
						   .recipient(recipient)
						   .type(type)
						   .referenceType(referenceType)
						   .referenceId(referenceId)
						   .parentReferenceId(parentReferenceId)
						   .message(message)
						   .build();
	}

	public void markAsRead() {
		this.isRead = true;
	}
}
