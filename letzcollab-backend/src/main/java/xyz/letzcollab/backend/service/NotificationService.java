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
import xyz.letzcollab.backend.dto.notification.NotificationSseEvent;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.global.sse.SseEmitterService;
import xyz.letzcollab.backend.repository.NotificationRepository;
import xyz.letzcollab.backend.repository.UserRepository;

import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.NOTIFICATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotifCntCacheService cacheService;
	private final SseEmitterService emitterService;

	@Transactional
	public Notification create(NotificationEvent event) {
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

		Notification saved = notificationRepository.save(notification);
		log.info("알림 생성 - type={}, recipientId={}, referenceType={}, referenceId={}, parentReferenceId={}",
				event.type(), event.recipientId(), event.referenceType(), event.referenceId(), event.parentReferenceId());
		return saved;
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getMyNotifications(UUID userPublicId, Pageable pageable) {
		Pageable sorted = PageRequest.of(
				pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt")
		);

		return notificationRepository.findByRecipientPublicId(userPublicId, sorted)
									 .map(NotificationResponse::from);
	}

	@Transactional
	public void markAsRead(UUID userPublicId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndRecipientPublicId(notificationId, userPublicId)
														  .orElseThrow(() -> new CustomException(NOTIFICATION_NOT_FOUND));

		notification.markAsRead();
		cacheService.decrement(userPublicId);
		log.info("알림 읽음 처리 - userId={}, notificationId={}", userPublicId, notificationId);
	}

	@Transactional
	public void markAllAsRead(UUID userPublicId) {
		int updatedCount = notificationRepository.markAllAsRead(userPublicId);
		cacheService.resetToZero(userPublicId);
		log.info("알림 전체 읽음 처리 - userId={}, count={}", userPublicId, updatedCount);
	}

	@Transactional(readOnly = true)
	public long getUnreadCount(UUID userPublicId) {
		return cacheService.get(userPublicId);
	}

	public void sendInitialUnread(UUID userPublicId) {
		long unreadCount = cacheService.get(userPublicId);
		emitterService.send(userPublicId, "init", new NotificationSseEvent(null, unreadCount));
	}
}