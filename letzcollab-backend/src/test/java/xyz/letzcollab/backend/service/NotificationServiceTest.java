package xyz.letzcollab.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.notification.NotificationResponse;
import xyz.letzcollab.backend.entity.Notification;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ReferenceType;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.NotificationRepository;
import xyz.letzcollab.backend.repository.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static xyz.letzcollab.backend.global.exception.ErrorCode.NOTIFICATION_NOT_FOUND;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@DisplayName("NotificationService 통합 테스트")
class NotificationServiceTest {

	@Autowired NotificationService notificationService;
	@Autowired NotificationRepository notificationRepository;
	@Autowired UserRepository userRepository;

	private User recipient;
	private User otherUser;

	private static final UUID SAMPLE_REFERENCE_ID = UUID.randomUUID();
	private static final UUID SAMPLE_PARENT_REFERENCE_ID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		recipient = saveUser("recipient@test.com", "알림 수신자");
		otherUser = saveUser("other@test.com", "다른 유저");
	}

	@Nested
	@DisplayName("알림 생성")
	class Create {

		@Test
		@DisplayName("업무 할당 알림이 생성된다")
		void createTaskAssignedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.TASK_ASSIGNED,
					ReferenceType.TASK, SAMPLE_REFERENCE_ID, SAMPLE_PARENT_REFERENCE_ID,
					"'API 설계' 업무가 할당되었습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getRecipient().getId().equals(recipient.getId()))
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.TASK);
			assertThat(saved.getReferenceId()).isEqualTo(SAMPLE_REFERENCE_ID);
			assertThat(saved.getParentReferenceId()).isEqualTo(SAMPLE_PARENT_REFERENCE_ID);
			assertThat(saved.getMessage()).contains("API 설계");
			assertThat(saved.isRead()).isFalse();
		}

		@Test
		@DisplayName("업무 상태 변경 알림이 생성된다")
		void createTaskStatusChangedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.TASK_STATUS_CHANGED,
					ReferenceType.TASK, SAMPLE_REFERENCE_ID, SAMPLE_PARENT_REFERENCE_ID,
					"'API 설계' 업무 상태가 IN_PROGRESS로 변경되었습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.TASK_STATUS_CHANGED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.TASK);
		}

		@Test
		@DisplayName("업무 재할당 알림이 생성된다")
		void createTaskReassignedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.TASK_REASSIGNED,
					ReferenceType.TASK, SAMPLE_REFERENCE_ID, SAMPLE_PARENT_REFERENCE_ID,
					"'API 설계' 업무가 다른 담당자에게 재할당되었습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.TASK_REASSIGNED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.TASK);
		}

		@Test
		@DisplayName("댓글 알림이 생성된다")
		void createCommentAddedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.COMMENT_ADDED,
					ReferenceType.TASK, SAMPLE_REFERENCE_ID, SAMPLE_PARENT_REFERENCE_ID,
					"'버그 수정' 업무에 새 댓글이 달렸습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.COMMENT_ADDED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.TASK);
		}

		@Test
		@DisplayName("대댓글 알림이 생성된다")
		void createCommentReplyAddedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.COMMENT_REPLY_ADDED,
					ReferenceType.TASK, SAMPLE_REFERENCE_ID, SAMPLE_PARENT_REFERENCE_ID,
					"'버그 수정' 업무에서 내 댓글에 답글이 달렸습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.COMMENT_REPLY_ADDED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.TASK);
		}

		@Test
		@DisplayName("프로젝트 멤버 추가 알림이 생성된다")
		void createProjectMemberAddedNotification() {
			UUID projectId = UUID.randomUUID();
			UUID workspaceId = UUID.randomUUID();
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.PROJECT_MEMBER_ADDED,
					ReferenceType.PROJECT, projectId, workspaceId,
					"'백엔드 리팩토링' 프로젝트에 멤버로 추가되었습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.PROJECT_MEMBER_ADDED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.PROJECT);
			assertThat(saved.getReferenceId()).isEqualTo(projectId);
			assertThat(saved.getParentReferenceId()).isEqualTo(workspaceId);
		}

		@Test
		@DisplayName("프로젝트 멤버 제외 알림이 생성된다")
		void createProjectMemberRemovedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.PROJECT_MEMBER_REMOVED,
					ReferenceType.PROJECT, UUID.randomUUID(), UUID.randomUUID(),
					"'백엔드 리팩토링' 프로젝트에서 제외되었습니다."
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.PROJECT_MEMBER_REMOVED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.PROJECT);
		}

		@Test
		@DisplayName("프로젝트 권한 변경 알림이 생성된다")
		void createProjectRoleChangedNotification() {
			NotificationEvent event = new NotificationEvent(
					recipient.getId(), NotificationType.PROJECT_ROLE_CHANGED,
					ReferenceType.PROJECT, UUID.randomUUID(), UUID.randomUUID(),
					"'백엔드 리팩토링' 프로젝트에서 '관리자' 관리자로 권한이 변경되었습니다"
			);

			notificationService.create(event);

			Notification saved = notificationRepository.findAll().stream()
													   .filter(n -> n.getType() == NotificationType.PROJECT_ROLE_CHANGED)
													   .findFirst()
													   .orElseThrow();

			assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.PROJECT);
		}
	}

	@Nested
	@DisplayName("알림 목록 조회")
	class GetMyNotifications {

		@Test
		@DisplayName("본인의 알림만 조회된다")
		void returnsOnlyMyNotifications() {
			createSampleNotification(recipient);
			createSampleNotification(recipient);
			createSampleNotification(otherUser);

			Page<NotificationResponse> result = notificationService.getMyNotifications(
					recipient.getPublicId(), PageRequest.of(0, 10)
			);

			assertThat(result.getTotalElements()).isEqualTo(2);
		}

		@Test
		@DisplayName("알림이 없으면 빈 페이지를 반환한다")
		void returnsEmptyPage() {
			Page<NotificationResponse> result = notificationService.getMyNotifications(
					recipient.getPublicId(), PageRequest.of(0, 10)
			);

			assertThat(result.getTotalElements()).isZero();
		}
	}

	@Nested
	@DisplayName("단건 읽음 처리")
	class MarkAsRead {

		@Test
		@DisplayName("알림을 읽음 처리하면 isRead가 true가 된다")
		void marksAsRead() {
			Notification notification = createSampleNotification(recipient);

			notificationService.markAsRead(recipient.getPublicId(), notification.getId());

			Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
			assertThat(updated.isRead()).isTrue();
		}

		@Test
		@DisplayName("다른 사용자의 알림에 읽음 처리 시도하면 NOTIFICATION_NOT_FOUND")
		void cannotReadOthersNotification() {
			Notification notification = createSampleNotification(otherUser);

			assertThatThrownBy(() ->
					notificationService.markAsRead(recipient.getPublicId(), notification.getId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(NOTIFICATION_NOT_FOUND);;
		}
	}

	@Nested
	@DisplayName("전체 읽음 처리")
	class MarkAllAsRead {

		@Test
		@DisplayName("읽지 않은 알림을 전부 읽음 처리한다")
		void marksAllAsRead() {
			createSampleNotification(recipient);
			createSampleNotification(recipient);
			createSampleNotification(recipient);

			notificationService.markAllAsRead(recipient.getPublicId());

			long unreadCount = notificationRepository.countByRecipientPublicIdAndIsReadFalse(recipient.getPublicId());
			assertThat(unreadCount).isZero();
		}

		@Test
		@DisplayName("다른 사용자의 알림에는 영향을 주지 않는다")
		void doesNotAffectOthers() {
			createSampleNotification(recipient);
			createSampleNotification(otherUser);

			notificationService.markAllAsRead(recipient.getPublicId());

			long otherUnread = notificationRepository.countByRecipientPublicIdAndIsReadFalse(otherUser.getPublicId());
			assertThat(otherUnread).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("읽지 않은 알림 개수 조회")
	class GetUnreadCount {

		@Test
		@DisplayName("읽지 않은 알림 개수를 정확히 반환한다")
		void returnsUnreadCount() {
			createSampleNotification(recipient);
			createSampleNotification(recipient);

			Notification read = createSampleNotification(recipient);
			read.markAsRead();

			long count = notificationService.getUnreadCount(recipient.getPublicId());
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("모든 알림을 읽었으면 0을 반환한다")
		void returnsZeroWhenAllRead() {
			Notification n = createSampleNotification(recipient);
			n.markAsRead();

			long count = notificationService.getUnreadCount(recipient.getPublicId());
			assertThat(count).isZero();
		}
	}



	// 헬퍼
	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "password1234!", null));
	}

	private Notification createSampleNotification(User user) {
		return notificationRepository.save(
				Notification.create(
						user,
						NotificationType.TASK_ASSIGNED,
						ReferenceType.TASK,
						SAMPLE_REFERENCE_ID,
						SAMPLE_PARENT_REFERENCE_ID,
						"테스트 알림 메시지"
				)
		);
	}
}