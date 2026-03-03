package xyz.letzcollab.backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("WorkspaceMember 도메인 로직")
class WorkspaceMemberTest {

	@Nested
	@DisplayName("초대 로직 검증")
	class CanInvite {

		@Test
		@DisplayName("OWNER는 초대 가능")
		void ownerCanInvite() {
			assertThat(ownerMember().canInvite()).isTrue();
		}

		@Test
		@DisplayName("ADMIN은 초대 가능")
		void adminCanInvite() {
			assertThat(adminMember().canInvite()).isTrue();
		}

		@Test
		@DisplayName("MEMBER는 초대 불가")
		void memberCannotInvite() {
			assertThat(generalMember().canInvite()).isFalse();
		}

		@Test
		@DisplayName("GUEST는 초대 불가")
		void guestCannotInvite() {
			assertThat(guestMember().canInvite()).isFalse();
		}
	}

	@Nested
	@DisplayName("타 멤버 수정 권한 검증")
	class CanUpdateOtherMember {

		@Test
		@DisplayName("OWNER는 ADMIN을 MEMBER로 강등 가능")
		void ownerCanDemoteAdmin() {
			assertThat(ownerMember().canUpdateOtherMember(adminMember(), WorkspaceRole.MEMBER)).isTrue();
		}

		@Test
		@DisplayName("OWNER는 ADMIN을 OWNER로 승격 불가 (소유권 이전 전용 메소드를 사용해야 함)")
		void ownerCannotPromoteOwner() {
			assertThat(ownerMember().canUpdateOtherMember(adminMember(), WorkspaceRole.OWNER)).isFalse();
		}

		@Test
		@DisplayName("ADMIN은 MEMBER를 ADMIN으로 승격 불가 (동급 부여 불가)")
		void adminCannotPromoteToAdmin() {
			assertThat(adminMember().canUpdateOtherMember(generalMember(), WorkspaceRole.ADMIN)).isFalse();
		}

		@Test
		@DisplayName("ADMIN은 MEMBER를 GUEST로 강등 가능 (하위 권한 부여 가능)")
		void adminCanDemoteMemberToGuest() {
			assertThat(adminMember().canUpdateOtherMember(generalMember(), WorkspaceRole.GUEST)).isTrue();
		}

		@Test
		@DisplayName("ADMIN은 MEMBER 직책 수정 가능 (권한 변경 없음)")
		void adminCanUpdateMemberPosition() {
			assertThat(adminMember().canUpdateOtherMember(generalMember(), null)).isTrue();
		}

		@Test
		@DisplayName("MEMBER는 타인 수정 불가")
		void memberCannotUpdateOthers() {
			assertThat(generalMember().canUpdateOtherMember(guestMember(), null)).isFalse();
		}

		@Test
		@DisplayName("ADMIN은 OWNER 수정 불가 (더 높은 권한)")
		void adminCannotUpdateOwner() {
			assertThat(adminMember().canUpdateOtherMember(ownerMember(), null)).isFalse();
		}
	}

	@Nested
	@DisplayName("강퇴 권한 로직 검증")
	class CanKickMember {

		@Test
		@DisplayName("OWNER는 ADMIN 강퇴 가능")
		void ownerCanKickAdmin() {
			assertThat(ownerMember().canKickMember(adminMember())).isTrue();
		}

		@Test
		@DisplayName("ADMIN은 MEMBER와 GUEST 강퇴 가능")
		void adminCanKickMember() {
			assertThat(adminMember().canKickMember(generalMember())).isTrue();
			assertThat(adminMember().canKickMember(guestMember())).isTrue();
		}

		@Test
		@DisplayName("ADMIN은 동급 ADMIN 강퇴 불가")
		void adminCannotKickAdmin() {
			assertThat(adminMember().canKickMember(adminMember())).isFalse();
		}

		@Test
		@DisplayName("MEMBER는 GUEST 강퇴 불가")
		void memberCannotKickGuest() {
			assertThat(generalMember().canKickMember(guestMember())).isFalse();
		}

		@Test
		@DisplayName("ADMIN은 OWNER 강퇴 불가")
		void adminCannotKickOwner() {
			assertThat(adminMember().canKickMember(ownerMember())).isFalse();
		}
	}

	@Nested
	@DisplayName("본인 정보 수정")
	class UpdatePosition {

		@Test
		@DisplayName("유효한 값으로 직책 변경")
		void updateToValid() {
			WorkspaceMember member = generalMember();
			member.updatePosition("PM");
			assertThat(member.getPosition()).isEqualTo("PM");
		}

		@Test
		@DisplayName("null 입력 시 기존 직책 유지")
		void nullKeepsOriginal() {
			WorkspaceMember member = generalMember();
			member.updatePosition(null);
			assertThat(member.getPosition()).isEqualTo("개발자");
		}

		@Test
		@DisplayName("빈 문자열 입력 시 기존 직책 유지")
		void blankKeepsOriginal() {
			WorkspaceMember member = generalMember();
			member.updatePosition("   ");
			assertThat(member.getPosition()).isEqualTo("개발자");
		}
	}


	// 헬퍼
	private WorkspaceMember ownerMember() {
		User user = mock(User.class);
		Workspace workspace = mock(Workspace.class);
		return WorkspaceMember.createWorkspaceOwner(user, workspace, "CTO");
	}

	private WorkspaceMember adminMember() {
		User user = mock(User.class);
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember admin = WorkspaceMember.createGeneralMember(user, workspace, "팀장");
		// ADMIN 역할로 올려주는 방법: updateRoleBySystem은 protected이므로 같은 패키지에서 접근
		admin.updateRoleBySystem(WorkspaceRole.ADMIN);
		return admin;
	}

	private WorkspaceMember generalMember() {
		User user = mock(User.class);
		Workspace workspace = mock(Workspace.class);
		return WorkspaceMember.createGeneralMember(user, workspace, "개발자");
	}

	private WorkspaceMember guestMember() {
		User user = mock(User.class);
		Workspace workspace = mock(Workspace.class);
		WorkspaceMember guest = WorkspaceMember.createGeneralMember(user, workspace, "외부");
		guest.updateRoleBySystem(WorkspaceRole.GUEST);
		return guest;
	}
}