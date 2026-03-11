package xyz.letzcollab.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.vo.NotificationType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByRecipientPublicId(UUID recipientPublicId, Pageable pageable);

	Optional<Notification> findByIdAndRecipientPublicId(Long id, UUID recipientPublicId);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true " +
			"WHERE n.recipient.publicId = :recipientPublicId AND n.isRead = false")
	int markAllAsRead(@Param("recipientPublicId") UUID recipientPublicId);

	long countByRecipientPublicIdAndIsReadFalse(UUID recipientPublicId);

	// 스케줄러 중복 방지용
	boolean existsByRecipientIdAndReferenceIdAndType(Long recipientId, UUID referenceId, NotificationType type);

	// 오래된 읽은 알림 벌크 삭제용
	@Modifying
	@Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
	int deleteReadNotificationsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}