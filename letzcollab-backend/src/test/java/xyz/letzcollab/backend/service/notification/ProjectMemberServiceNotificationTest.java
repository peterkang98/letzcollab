package xyz.letzcollab.backend.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.CreateProjectRequest;
import xyz.letzcollab.backend.dto.project.UpdateOtherMemberRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.NotificationType;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.global.event.dto.NotificationEvent;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;
import xyz.letzcollab.backend.service.ProjectMemberService;
import xyz.letzcollab.backend.service.ProjectService;
import xyz.letzcollab.backend.service.WorkspaceService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@RecordApplicationEvents
@DisplayName("ProjectMemberService 알림 이벤트 통합 테스트")
class ProjectMemberServiceNotificationTest {

	@Autowired
	ProjectMemberService projectMemberService;
	@Autowired
	ProjectService projectService;
	@Autowired
	WorkspaceService workspaceService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	WorkspaceRepository workspaceRepository;
	@Autowired
	WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	ApplicationEvents events;

	private User wsOwner;
	private User targetUser;
	private Workspace workspace;
	private UUID projectId;

	@BeforeEach
	void setUp() {
		wsOwner = saveUser("owner@test.com", "소유자");
		targetUser = saveUser("target@test.com", "대상 유저");

		workspaceService.createWorkspace(wsOwner.getPublicId(), "테스트 워크스페이스", "CTO");
		workspace = workspaceRepository.findAll().stream()
									   .filter(w -> w.getName().equals("테스트 워크스페이스"))
									   .findFirst().orElseThrow();

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(targetUser, workspace, "개발자"));

		projectId = projectService.createProject(wsOwner.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest("테스트 프로젝트", null, ProjectStatus.ACTIVE, null, null, false, null));

		events.clear();
	}

	@Nested
	@DisplayName("멤버 추가 알림")
	class AddMember {

		@Test
		@DisplayName("멤버를 추가하면 PROJECT_MEMBER_ADDED 이벤트가 발행된다")
		void addMemberNotification() {
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(targetUser.getPublicId(), ProjectRole.MEMBER, null));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().type()).isEqualTo(NotificationType.PROJECT_MEMBER_ADDED);
			assertThat(fired.getFirst().recipientId()).isEqualTo(targetUser.getId());
		}
	}

	@Nested
	@DisplayName("권한 변경 알림")
	class RoleChanged {

		@Test
		@DisplayName("멤버 권한을 변경하면 PROJECT_ROLE_CHANGED 이벤트가 발행된다")
		void roleChangeNotification() {
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(targetUser.getPublicId(), ProjectRole.MEMBER, null));
			events.clear();

			projectMemberService.updateOtherMember(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId,
					new UpdateOtherMemberRequest(targetUser.getPublicId(), null, ProjectRole.ADMIN));

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().type()).isEqualTo(NotificationType.PROJECT_ROLE_CHANGED);
			assertThat(fired.getFirst().recipientId()).isEqualTo(targetUser.getId());
		}

		@Test
		@DisplayName("직책만 변경하고 권한은 변경하지 않으면 알림이 발행되지 않는다")
		void positionOnlyChange() {
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(targetUser.getPublicId(), ProjectRole.MEMBER, null));
			events.clear();

			projectMemberService.updateOtherMember(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId,
					new UpdateOtherMemberRequest(targetUser.getPublicId(), "새 직책", null));

			long count = events.stream(NotificationEvent.class).count();
			assertThat(count).isZero();
		}
	}

	@Nested
	@DisplayName("멤버 강퇴 알림")
	class KickMember {

		@Test
		@DisplayName("멤버를 강퇴하면 PROJECT_MEMBER_REMOVED 이벤트가 발행된다")
		void kickMemberNotification() {
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId,
					new AddMemberRequest(targetUser.getPublicId(), ProjectRole.MEMBER, null));
			events.clear();

			projectMemberService.kickMember(
					wsOwner.getPublicId(), workspace.getPublicId(), targetUser.getPublicId(), projectId);

			List<NotificationEvent> fired = events.stream(NotificationEvent.class).toList();
			assertThat(fired).hasSize(1);
			assertThat(fired.getFirst().type()).isEqualTo(NotificationType.PROJECT_MEMBER_REMOVED);
			assertThat(fired.getFirst().recipientId()).isEqualTo(targetUser.getId());
		}
	}

	// 헬퍼
	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "password1234!", null));
	}
}