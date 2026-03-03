package xyz.letzcollab.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceInvitation;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;
import xyz.letzcollab.backend.global.email.EmailService;
import xyz.letzcollab.backend.global.email.context.VerifyEmailContext;
import xyz.letzcollab.backend.global.email.context.WorkspaceInvitationEmailContext;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceInvitationRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static xyz.letzcollab.backend.global.exception.ErrorCode.*;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@DisplayName("WorkspaceMemberService 통합 테스트")
class WorkspaceMemberServiceTest {
	@Autowired
	WorkspaceMemberService memberService;
	@Autowired
	WorkspaceService workspaceService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	WorkspaceRepository workspaceRepository;
	@Autowired
	WorkspaceMemberRepository memberRepository;
	@Autowired
	WorkspaceInvitationRepository invitationRepository;

	// 이메일 실제 발송 방지
	@MockitoBean
	EmailService emailService;

	private User owner;
	private User adminUser;
	private User generalUser;
	private User outsider;
	private Workspace workspace;

	@BeforeEach
	void setUp() {
		owner = saveUser("owner@test.com", "소유자");
		adminUser = saveUser("admin@test.com", "관리자");
		generalUser = saveUser("general@test.com", "일반 회원");
		outsider = saveUser("outsider@test.com", "외부 협력자");

		// 워크스페이스 생성
		workspaceService.createWorkspace(owner.getPublicId(), "우아한동네", "CTO");
		workspace = findWorkspaceByName("우아한동네");

		// 멤버 추가 후 관리자로 승격
		workspace.addMember(adminUser, "팀장");
		memberService.updateOtherMember(
				owner.getPublicId(), workspace.getPublicId(), adminUser.getPublicId(), null, WorkspaceRole.ADMIN
		);

		// 일반 회원 추가
		workspace.addMember(generalUser, "개발자");
		workspaceRepository.saveAndFlush(workspace);
	}

	@Nested
	@DisplayName("email 주소로 멤버 초대")
	class InviteMemberByEmail {

		@Test
		@DisplayName("OWNER가 초대하면 WorkspaceInvitation이 저장되고 이메일이 발송된다")
		void ownerCanInvite() {
			memberService.inviteMemberByEmail(
					owner.getPublicId(), workspace.getPublicId(), "new@test.com", "디자이너");

			assertThat(invitationRepository.findAll())
					.anyMatch(inv -> inv.getInviteeEmail().equals("new@test.com"));

			verify(emailService, times(1)).sendTemplateEmail(
					eq("new@test.com"), any(WorkspaceInvitationEmailContext.class)
			);
		}

		@Test
		@DisplayName("ADMIN도 초대 가능하다")
		void adminCanInvite() {
			memberService.inviteMemberByEmail(adminUser.getPublicId(), workspace.getPublicId(), "new@test.com", null);

			assertThat(invitationRepository.findAll()).hasSize(1);

			verify(emailService, times(1)).sendTemplateEmail(
					eq("new@test.com"), any(WorkspaceInvitationEmailContext.class)
			);
		}

		@Test
		@DisplayName("MEMBER는 초대 불가 → INSUFFICIENT_WORKSPACE_PERMISSION 오류코드 반환")
		void memberCannotInvite() {
			assertThatThrownBy(() ->
					memberService.inviteMemberByEmail(
							generalUser.getPublicId(), workspace.getPublicId(), "new@test.com", null))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		@Test
		@DisplayName("이미 멤버인 이메일로 초대하면 ALREADY_A_WORKSPACE_MEMBER 오류코드 반환")
		void alreadyMember() {
			assertThatThrownBy(() ->
					memberService.inviteMemberByEmail(
							owner.getPublicId(), workspace.getPublicId(), generalUser.getEmail(), null))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(ALREADY_A_WORKSPACE_MEMBER);
		}
	}

	@Nested
	@DisplayName("초대 수락")
	class AcceptInvitation {

		private WorkspaceInvitation validInvitation;

		@BeforeEach
		void createInvitation() {
			validInvitation = saveInvitation(outsider.getEmail(), false);
		}

		@Test
		@DisplayName("유효한 초대를 수락하면 멤버로 추가되고 usedAt이 기록된다")
		void success() {
			memberService.acceptInvitation(outsider.getPublicId(), validInvitation.getToken());

			assertThat(memberRepository.existsByWorkspaceAndUser(workspace, outsider)).isTrue();
			WorkspaceInvitation used = invitationRepository.findById(validInvitation.getId()).orElseThrow();
			assertThat(used.getUsedAt()).isNotNull();
		}

		@Test
		@DisplayName("만료된 초대는 WORKSPACE_INVITATION_INVALID 오류코드 반환")
		void expiredInvitation() {
			WorkspaceInvitation expired = saveInvitation(outsider.getEmail(), true);

			assertThatThrownBy(() ->
					memberService.acceptInvitation(outsider.getPublicId(), expired.getToken()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_INVITATION_INVALID);
		}

		@Test
		@DisplayName("이미 사용된 초대는 WORKSPACE_INVITATION_INVALID 오류코드 반환")
		void alreadyUsed() {
			memberService.acceptInvitation(outsider.getPublicId(), validInvitation.getToken());

			// 두 번 수락 시도
			assertThatThrownBy(() ->
					memberService.acceptInvitation(outsider.getPublicId(), validInvitation.getToken()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_INVITATION_INVALID);
		}

		@Test
		@DisplayName("초대받은 이메일과 다른 계정으로 수락하면 WORKSPACE_INVITEE_MISMATCH 오류코드 반환")
		void emailMismatch() {
			assertThatThrownBy(() ->
					memberService.acceptInvitation(generalUser.getPublicId(), validInvitation.getToken()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_INVITEE_MISMATCH);
		}
	}

	@Nested
	@DisplayName("본인 정보 수정")
	class UpdateMyself {

		@Test
		@DisplayName("본인 직책을 수정할 수 있다")
		void updateOwnPosition() {
			memberService.updateMyself(workspace.getPublicId(), generalUser.getPublicId(), "PM");

			WorkspaceMember me = memberRepository
					.findByUserAndWorkspacePublicId(workspace.getPublicId(), generalUser.getPublicId())
					.orElseThrow();
			assertThat(me.getPosition()).isEqualTo("PM");
		}

		@Test
		@DisplayName("빈 문자열로 직책 수정 시 기존 직책 유지")
		void blankPositionKeepsOriginal() {
			memberService.updateMyself(workspace.getPublicId(), generalUser.getPublicId(), "   ");

			WorkspaceMember me = memberRepository.findByUserAndWorkspacePublicId(workspace.getPublicId(), generalUser.getPublicId())
												 .orElseThrow();
			assertThat(me.getPosition()).isEqualTo("개발자");
		}
	}

	@Nested
	@DisplayName("타 멤버 정보/권한 수정")
	class UpdateOtherMember {

		@Test
		@DisplayName("OWNER가 MEMBER의 역할과 직책을 수정한다")
		void ownerUpdatesGeneralMember() {
			memberService.updateOtherMember(
					owner.getPublicId(), workspace.getPublicId(),
					generalUser.getPublicId(), "PM", WorkspaceRole.ADMIN);

			WorkspaceMember target = memberRepository.findByUserAndWorkspacePublicId(workspace.getPublicId(), generalUser.getPublicId())
													 .orElseThrow();
			assertThat(target.getPosition()).isEqualTo("PM");
			assertThat(target.getRole()).isEqualTo(WorkspaceRole.ADMIN);
		}

		@Test
		@DisplayName("ADMIN은 MEMBER를 GUEST로 강등 가능 (하위 권한 부여 가능)")
		void adminCanDemoteMember() {
			memberService.updateOtherMember(
					adminUser.getPublicId(), workspace.getPublicId(),
					generalUser.getPublicId(), null, WorkspaceRole.GUEST);

			WorkspaceMember target = memberRepository.findByUserAndWorkspacePublicId(workspace.getPublicId(), generalUser.getPublicId())
													 .orElseThrow();
			assertThat(target.getRole()).isEqualTo(WorkspaceRole.GUEST);
		}

		@Test
		@DisplayName("ADMIN은 MEMBER를 ADMIN으로 승격 불가 (동급 권한 부여 불가) → INSUFFICIENT_WORKSPACE_PERMISSION 오류코드 반환")
		void adminCannotPromoteToAdmin() {
			assertThatThrownBy(() ->
					memberService.updateOtherMember(
							adminUser.getPublicId(), workspace.getPublicId(),
							generalUser.getPublicId(), null, WorkspaceRole.ADMIN))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		@Test
		@DisplayName("본인 ID로 호출하면 USE_SELF_UPDATE_API 오류코드 반환")
		void selfUpdate() {
			assertThatThrownBy(() ->
					memberService.updateOtherMember(
							owner.getPublicId(), workspace.getPublicId(),
							owner.getPublicId(), "새직책", null))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(USE_SELF_UPDATE_API);
		}

		@Test
		@DisplayName("워크스페이스에 없는 유저를 대상으로 하면 WORKSPACE_MEMBER_NOT_FOUND 오류코드 반환")
		void targetNotFound() {
			assertThatThrownBy(() ->
					memberService.updateOtherMember(
							owner.getPublicId(), workspace.getPublicId(),
							outsider.getPublicId(), null, WorkspaceRole.MEMBER))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_MEMBER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("자진 탈퇴")
	class LeaveWorkspace {

		@Test
		@DisplayName("일반 멤버가 자진 탈퇴하면 멤버 목록에서 제거된다")
		void memberCanLeave() {
			memberService.leaveWorkspace(generalUser.getPublicId(), workspace.getPublicId());

			assertThat(memberRepository.existsByWorkspaceAndUser(workspace, generalUser)).isFalse();
		}

		@Test
		@DisplayName("OWNER는 소유권 이전 없이 탈퇴 불가 → WORKSPACE_OWNER_RELEASE_REQUIRED 오류코드 반환")
		void ownerCannotLeave() {
			assertThatThrownBy(() ->
					memberService.leaveWorkspace(owner.getPublicId(), workspace.getPublicId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_OWNER_RELEASE_REQUIRED);
		}
	}

	@Nested
	@DisplayName("강퇴")
	class KickMember {

		@Test
		@DisplayName("OWNER가 ADMIN을 강퇴하면 멤버 목록에서 제거된다")
		void ownerKicksAdmin() {
			memberService.kickMember(owner.getPublicId(), workspace.getPublicId(), adminUser.getPublicId());

			assertThat(memberRepository.existsByWorkspaceAndUser(workspace, adminUser)).isFalse();
		}

		@Test
		@DisplayName("ADMIN이 MEMBER를 강퇴할 수 있다")
		void adminKicksMember() {
			memberService.kickMember(adminUser.getPublicId(), workspace.getPublicId(), generalUser.getPublicId());

			assertThat(memberRepository.existsByWorkspaceAndUser(workspace, generalUser)).isFalse();
		}

		@Test
		@DisplayName("MEMBER가 강퇴를 시도하면 INSUFFICIENT_WORKSPACE_PERMISSION 오류코드 반환")
		void memberCannotKick() {
			assertThatThrownBy(() ->
					memberService.kickMember(generalUser.getPublicId(), workspace.getPublicId(), adminUser.getPublicId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		@Test
		@DisplayName("ADMIN이 동급 ADMIN을 강퇴 불가 → INSUFFICIENT_WORKSPACE_PERMISSION 오류코드 반환")
		void adminCannotKickAdmin() {
			// given
			User admin2 = saveUser("admin2@test.com", "어드민2");
			workspace.addMember(admin2, "부팀장");
			memberService.updateOtherMember(
					owner.getPublicId(), workspace.getPublicId(), admin2.getPublicId(), null, WorkspaceRole.ADMIN
			);

			// when & then
			assertThatThrownBy(() ->
					memberService.kickMember(adminUser.getPublicId(), workspace.getPublicId(), admin2.getPublicId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		@Test
		@DisplayName("본인을 강퇴 시도하면 USE_SELF_DELETE_API 오류코드 반환")
		void selfKick() {
			assertThatThrownBy(() ->
					memberService.kickMember(owner.getPublicId(), workspace.getPublicId(), owner.getPublicId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(USE_SELF_DELETE_API);
		}
	}

	@Nested
	@DisplayName("소유자 권한 이전")
	class TransferOwnership {

		@Test
		@DisplayName("소유권 이전 후 신규 OWNER의 역할이 OWNER가 되고 기존 OWNER는 ADMIN이 된다")
		void transferSuccess() {
			// given & when
			memberService.transferOwnership(
					owner.getPublicId(), workspace.getPublicId(), adminUser.getPublicId());

			// then
			Workspace updated = workspaceRepository.findWorkspaceWithAllMembers(workspace.getPublicId()).orElseThrow();
			assertThat(updated.getOwner().getPublicId()).isEqualTo(adminUser.getPublicId());

			WorkspaceMember newOwner = updated.getMembers().stream()
											  .filter(m -> m.getUser().getPublicId().equals(adminUser.getPublicId()))
											  .findFirst().orElseThrow();
			WorkspaceMember oldOwner = updated.getMembers().stream()
											  .filter(m -> m.getUser().getPublicId().equals(owner.getPublicId()))
											  .findFirst().orElseThrow();

			assertThat(newOwner.getRole()).isEqualTo(WorkspaceRole.OWNER);
			assertThat(oldOwner.getRole()).isEqualTo(WorkspaceRole.ADMIN);
		}

		@Test
		@DisplayName("자기 자신에게 이전하면 CANNOT_TRANSFER_OWNERSHIP_TO_SELF 오류코드 반환")
		void selfTransfer() {
			assertThatThrownBy(() ->
					memberService.transferOwnership(
							owner.getPublicId(), workspace.getPublicId(), owner.getPublicId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(CANNOT_TRANSFER_OWNERSHIP_TO_SELF);
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아닌 유저에게 이전하면 WORKSPACE_MEMBER_NOT_FOUND 오류코드 반환")
		void targetNotMember() {
			assertThatThrownBy(() ->
					memberService.transferOwnership(
							owner.getPublicId(), workspace.getPublicId(), outsider.getPublicId()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_MEMBER_NOT_FOUND);
		}
	}


	// 헬퍼
	private User saveUser(String email, String name) {
		return userRepository.save(
				User.createDummyUser(name, email, "password1234!", null)
		);
	}

	private Workspace findWorkspaceByName(String name) {
		return workspaceRepository.findAll().stream()
								  .filter(w -> w.getName().equals(name))
								  .findFirst()
								  .orElseThrow();
	}

	private WorkspaceInvitation saveInvitation(String inviteeEmail, boolean expired) {
		WorkspaceInvitation invitation = WorkspaceInvitation.createWorkspaceInvitation(
				owner, workspace, inviteeEmail, "디자이너"
		);
		if (expired) {
			// expiresAt을 과거로 조작
			setField(invitation, "expiresAt", LocalDateTime.now().minusSeconds(1));
		}
		return invitationRepository.save(invitation);
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}