package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.notification.NotificationResponse;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.repository.NotificationRepository;
import xyz.letzcollab.backend.repository.UserRepository;

import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.NOTIFICATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	public void create(NotificationEvent event) {
		User recipient = userRepository.findById(event.recipientId())
									   .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		Notification notification = Notification.create(
				recipient,
				event.type(),
				event.referenceType(),
				event.referenceId(),
				event.parentReferenceId(),
				event.message()
		);

		notificationRepository.save(notification);
		log.info("알림 생성 - type={}, recipientId={}, referenceType={}, referenceId={}, parentReferenceId={}",
				event.type(), event.recipientId(), event.referenceType(), event.referenceId(), event.parentReferenceId());
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getMyNotifications(UUID userPublicId, Pageable pageable) {
		Pageable sorted = PageRequest.of(
				pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt")
		);

		return notificationRepository.findByRecipientPublicId(userPublicId, sorted)
									 .map(NotificationResponse::from);
	}

	public void markAsRead(UUID userPublicId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndRecipientPublicId(notificationId, userPublicId)
														  .orElseThrow(() -> new CustomException(NOTIFICATION_NOT_FOUND));

		notification.markAsRead();
		log.info("알림 읽음 처리 - userId={}, notificationId={}", userPublicId, notificationId);
	}

	public void markAllAsRead(UUID userPublicId) {
		int updatedCount = notificationRepository.markAllAsRead(userPublicId);
		log.info("알림 전체 읽음 처리 - userId={}, count={}", userPublicId, updatedCount);
	}

	@Transactional(readOnly = true)
	public long getUnreadCount(UUID userPublicId) {
		return notificationRepository.countByRecipientPublicIdAndIsReadFalse(userPublicId);
	}
}