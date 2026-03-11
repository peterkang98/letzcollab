package xyz.letzcollab.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.CreateProjectRequest;
import xyz.letzcollab.backend.dto.project.UpdateMyselfRequest;
import xyz.letzcollab.backend.dto.project.UpdateOtherMemberRequest;
import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@DisplayName("ProjectMemberService 통합 테스트")
class ProjectMemberServiceTest {

	@MockitoBean
	ApplicationEventPublisher eventPublisher;

	@Autowired
	ProjectMemberService projectMemberService;
	@Autowired
	ProjectService projectService;
	@Autowired
	WorkspaceService workspaceService;
	@Autowired
	WorkspaceMemberService workspaceMemberService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	WorkspaceRepository workspaceRepository;
	@Autowired
	WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProjectMemberRepository projectMemberRepository;

	private User wsOwner;    // 워크스페이스 OWNER = 프로젝트 LEADER(ADMIN)
	private User wsAdmin;    // 워크스페이스 ADMIN = 프로젝트 ADMIN
	private User wsMember;   // 워크스페이스 MEMBER = 프로젝트 MEMBER
	private User wsOutsider; // 워크스페이스 MEMBER, 프로젝트 외부 협력자
	private User stranger;   // 워크스페이스 비소속 외부인

	private Workspace workspace;
	private UUID projectId;

	@BeforeEach
	void setUp() {
		wsOwner = saveUser("owner@test.com", "소유자");
		wsAdmin = saveUser("admin@test.com", "관리자");
		wsMember = saveUser("member@test.com", "일반 멤버");
		wsOutsider = saveUser("outsider@test.com", "외부인");
		stranger = saveUser("stranger@test.com", "타인");

		workspaceService.createWorkspace(wsOwner.getPublicId(), "테스트 워크스페이스", "CTO");
		workspace = findWorkspaceByName("테스트 워크스페이스");

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(wsAdmin, workspace, "팀장"));
		workspaceMemberService.updateOtherMember(
				wsOwner.getPublicId(), workspace.getPublicId(), wsAdmin.getPublicId(), null, WorkspaceRole.ADMIN
		);

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(wsMember, workspace, "개발자"));
		workspaceMemberRepository.saveAndFlush(WorkspaceMember.createGeneralMember(wsOutsider, workspace, "디자이너"));

		// 프로젝트 생성 (wsOwner가 LEADER이자 ADMIN으로 등록됨)
		projectId = projectService.createProject(wsOwner.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest("테스트 프로젝트", null, ProjectStatus.ACTIVE, null, null, false, null));

		// wsAdmin을 프로젝트 ADMIN으로 추가
		projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(wsAdmin.getPublicId(), ProjectRole.ADMIN, null));

		// wsMember를 프로젝트 MEMBER로 추가
		projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId,
				new AddMemberRequest(wsMember.getPublicId(), ProjectRole.MEMBER, null));
	}


	@Nested
	@DisplayName("프로젝트 멤버 추가")
	class AddMember {

		@Test
		@DisplayName("LEADER가 워크스페이스 멤버를 ADMIN으로 추가할 수 있다")
		void leaderCanAddAdmin() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsOutsider.getPublicId(), ProjectRole.ADMIN, "PM");
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicIdAndRole(
					projectId, wsOutsider.getPublicId(), ProjectRole.ADMIN)).isTrue();
		}

		@Test
		@DisplayName("LEADER가 워크스페이스 멤버를 VIEWER로 추가할 수 있다")
		void leaderCanAddViewer() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsOutsider.getPublicId(), ProjectRole.VIEWER, "외부 협력자");
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicIdAndRole(
					projectId, wsOutsider.getPublicId(), ProjectRole.VIEWER)).isTrue();
		}

		@Test
		@DisplayName("ADMIN도 워크스페이스 멤버를 MEMBER/VIEWER로 추가할 수 있다")
		void adminCanAddMember() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsOutsider.getPublicId(), ProjectRole.MEMBER, null);
			projectMemberService.addMember(wsAdmin.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicIdAndRole(
					projectId, wsOutsider.getPublicId(), ProjectRole.MEMBER)).isTrue();
		}

		@Test
		@DisplayName("LEADER가 아닌 ADMIN이 멤버를 추가할 때 ADMIN 권한 부여 불가 → INSUFFICIENT_PERMISSION")
		void adminCannotAddAdmin() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsOutsider.getPublicId(), ProjectRole.ADMIN, null);
			assertThatThrownBy(() -> projectMemberService.addMember(
					wsAdmin.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("일반 프로젝트 멤버는 멤버 추가 불가 → INSUFFICIENT_PERMISSION")
		void memberCannotAdd() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsOutsider.getPublicId(), ProjectRole.VIEWER, null);
			assertThatThrownBy(() -> projectMemberService.addMember(
					wsMember.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("이미 프로젝트 멤버인 사용자를 추가하면 ALREADY_A_PROJECT_MEMBER")
		void alreadyProjectMember() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsAdmin.getPublicId(), ProjectRole.MEMBER, null);
			assertThatThrownBy(() -> projectMemberService.addMember(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(ALREADY_A_PROJECT_MEMBER);
		}

		@Test
		@DisplayName("워크스페이스에 없는 사용자를 추가하면 WORKSPACE_MEMBER_NOT_FOUND")
		void workspaceMemberNotFound() {
			AddMemberRequest addMemberRequest = new AddMemberRequest(stranger.getPublicId(), ProjectRole.MEMBER, null);
			assertThatThrownBy(() -> projectMemberService.addMember(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_MEMBER_NOT_FOUND);
		}
	}


	@Nested
	@DisplayName("타 멤버 정보/권한 수정")
	class UpdateOtherMember {

		@Test
		@DisplayName("LEADER가 ADMIN의 직책과 권한을 수정할 수 있다")
		void leaderCanUpdateAdmin() {
			UpdateOtherMemberRequest updateReq = new UpdateOtherMemberRequest(wsAdmin.getPublicId(), "신규 포지션", ProjectRole.MEMBER);
			projectMemberService.updateOtherMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId, updateReq);

			ProjectMember target = projectMemberRepository.findByUserPublicIdAndProjectPublicId(wsAdmin.getPublicId(), projectId)
														  .orElseThrow();
			assertThat(target.getPosition()).isEqualTo("신규 포지션");
			assertThat(target.getRole()).isEqualTo(ProjectRole.MEMBER);
		}

		@Test
		@DisplayName("ADMIN이 MEMBER의 권한을 VIEWER로 변경할 수 있다")
		void adminCanDemoteMember() {
			UpdateOtherMemberRequest updateReq = new UpdateOtherMemberRequest(wsMember.getPublicId(), null, ProjectRole.VIEWER);
			projectMemberService.updateOtherMember(wsAdmin.getPublicId(), workspace.getPublicId(), projectId, updateReq);

			ProjectMember target = projectMemberRepository.findByUserPublicIdAndProjectPublicId(wsMember.getPublicId(), projectId)
														  .orElseThrow();
			assertThat(target.getRole()).isEqualTo(ProjectRole.VIEWER);
		}

		@Test
		@DisplayName("LEADER가 아닌 ADMIN은 다른 ADMIN을 수정 불가 → INSUFFICIENT_PERMISSION")
		void adminCannotUpdateAdmin() {
			// given
			User wsAdmin2 = saveUser("admin2@test.com", "관리자2");
			workspaceMemberRepository.saveAndFlush(WorkspaceMember.createGeneralMember(wsAdmin2, workspace, "부팀장"));

			AddMemberRequest addMemberRequest = new AddMemberRequest(wsAdmin2.getPublicId(), ProjectRole.ADMIN, null);
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest);

			// when & then
			assertThatThrownBy(() -> projectMemberService.updateOtherMember(
					wsAdmin.getPublicId(), workspace.getPublicId(), projectId,
					new UpdateOtherMemberRequest(wsAdmin2.getPublicId(), null, ProjectRole.MEMBER)))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("LEADER가 아닌 ADMIN은 ADMIN 권한 부여 불가 → INSUFFICIENT_PERMISSION")
		void adminCannotGrantAdminRole() {
			UpdateOtherMemberRequest updateReq = new UpdateOtherMemberRequest(wsMember.getPublicId(), null, ProjectRole.ADMIN);
			assertThatThrownBy(() -> projectMemberService.updateOtherMember(
					wsAdmin.getPublicId(), workspace.getPublicId(), projectId, updateReq))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("MEMBER가 타 멤버 수정 시도하면 INSUFFICIENT_PERMISSION")
		void memberCannotUpdate() {
			UpdateOtherMemberRequest updateReq = new UpdateOtherMemberRequest(wsAdmin.getPublicId(), "새 포지션", null);
			assertThatThrownBy(() -> projectMemberService.updateOtherMember(
					wsMember.getPublicId(), workspace.getPublicId(), projectId, updateReq))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("본인 ID로 호출하면 USE_SELF_UPDATE_API")
		void selfUpdate() {
			UpdateOtherMemberRequest updateReq = new UpdateOtherMemberRequest(wsOwner.getPublicId(), null, null);
			assertThatThrownBy(() -> projectMemberService.updateOtherMember(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId, updateReq))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(USE_SELF_UPDATE_API);
		}

		@Test
		@DisplayName("프로젝트에 없는 유저를 대상으로 하면 PROJECT_MEMBER_NOT_FOUND")
		void targetNotFound() {
			UpdateOtherMemberRequest updateReq = new UpdateOtherMemberRequest(wsOutsider.getPublicId(), null, ProjectRole.MEMBER);
			assertThatThrownBy(() -> projectMemberService.updateOtherMember(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId, updateReq))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_MEMBER_NOT_FOUND);
		}
	}


	@Nested
	@DisplayName("본인 정보 수정")
	class UpdateMyself {

		@Test
		@DisplayName("본인 직책을 수정할 수 있다")
		void updateOwnPosition() {
			projectMemberService.updateMyself(
					wsMember.getPublicId(), projectId, new UpdateMyselfRequest("백엔드 개발")
			);

			ProjectMember me = projectMemberRepository
					.findByUserPublicIdAndProjectPublicId(wsMember.getPublicId(), projectId).orElseThrow();
			assertThat(me.getPosition()).isEqualTo("백엔드 개발");
		}

		@Test
		@DisplayName("빈 문자열로 직책 수정 시 기존 직책 유지")
		void blankPositionKeepsOriginal() {
			// wsMember는 워크스페이스에서 "개발자" 포지션으로 등록됐으므로 프로젝트 position도 "개발자"
			projectMemberService.updateMyself(
					wsMember.getPublicId(), projectId, new UpdateMyselfRequest("   ")
			);

			ProjectMember me = projectMemberRepository
					.findByUserPublicIdAndProjectPublicId(wsMember.getPublicId(), projectId).orElseThrow();
			assertThat(me.getPosition()).isEqualTo("개발자");
		}

		@Test
		@DisplayName("프로젝트에 속하지 않은 멤버가 본인 정보 수정 시도 시 -> PROJECT_NOT_FOUND_OR_ACCESS_DENIED")
		void nonMemberCannotUpdate() {
			assertThatThrownBy(() -> projectMemberService.updateMyself(
					wsOutsider.getPublicId(), projectId, new UpdateMyselfRequest("역할")))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("자진 탈퇴")
	class LeaveProject {

		@Test
		@DisplayName("MEMBER가 자진 탈퇴하면 멤버 목록에서 제거된다")
		void memberCanLeave() {
			projectMemberService.leaveProject(wsMember.getPublicId(), projectId);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicId(
					projectId, wsMember.getPublicId())).isFalse();
		}

		@Test
		@DisplayName("리더가 아닌 ADMIN도 자진 탈퇴할 수 있다")
		void adminCanLeave() {
			projectMemberService.leaveProject(wsAdmin.getPublicId(), projectId);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicId(
					projectId, wsAdmin.getPublicId())).isFalse();
		}

		@Test
		@DisplayName("LEADER는 리더 변경 없이 탈퇴 불가 → PROJECT_LEADER_RELEASE_REQUIRED")
		void leaderCannotLeave() {
			assertThatThrownBy(() -> projectMemberService.leaveProject(wsOwner.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_LEADER_RELEASE_REQUIRED);
		}

		@Test
		@DisplayName("프로젝트에 속하지 않은 멤버가 탈퇴 시도 시 -> PROJECT_NOT_FOUND_OR_ACCESS_DENIED")
		void nonMemberCannotLeave() {
			assertThatThrownBy(() -> projectMemberService.leaveProject(wsOutsider.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("리더 변경")
	class ChangeLeader {

		@Test
		@DisplayName("LEADER가 ADMIN 멤버에게 리더 권한을 이전하면 project.leader가 변경된다")
		void leaderCanTransferToAdmin() {
			projectMemberService.changeLeader(wsOwner.getPublicId(), wsAdmin.getPublicId(), projectId);

			assertThat(projectRepository.findByPublicIdWithLeader(projectId).orElseThrow()
										.getLeader().getPublicId()).isEqualTo(wsAdmin.getPublicId());
		}

		@Test
		@DisplayName("MEMBER에게 리더 이전 후 해당 멤버의 ProjectRole이 ADMIN으로 승격된다")
		void newLeaderRoleIsAdmin() {
			projectMemberService.changeLeader(wsOwner.getPublicId(), wsMember.getPublicId(), projectId);

			ProjectMember newLeader = projectMemberRepository.findByUserPublicIdAndProjectPublicId(wsMember.getPublicId(), projectId)
															 .orElseThrow();
			assertThat(newLeader.getRole()).isEqualTo(ProjectRole.ADMIN);
		}

		@Test
		@DisplayName("LEADER가 아닌 유저가 리더 이전 시도 시 → INSUFFICIENT_PERMISSION")
		void nonLeaderCannotTransfer() {
			assertThatThrownBy(() ->
					projectMemberService.changeLeader(wsAdmin.getPublicId(), wsMember.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("LEADER가 자기 자신에게 리더 이전 시도 시 → CANNOT_CHANGE_LEADER_TO_SELF")
		void selfTransfer() {
			assertThatThrownBy(() ->
					projectMemberService.changeLeader(wsOwner.getPublicId(), wsOwner.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(CANNOT_CHANGE_LEADER_TO_SELF);
		}

		@Test
		@DisplayName("프로젝트에 속하지 않은 멤버에게 리더 이전 시도 시 -> PROJECT_MEMBER_NOT_FOUND")
		void targetNotProjectMember() {
			assertThatThrownBy(() ->
					projectMemberService.changeLeader(wsOwner.getPublicId(), wsOutsider.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_MEMBER_NOT_FOUND);
		}
	}


	@Nested
	@DisplayName("강퇴")
	class KickMember {

		@Test
		@DisplayName("LEADER가 ADMIN을 강퇴하면 멤버 목록에서 제거된다")
		void leaderCanKickAdmin() {
			projectMemberService.kickMember(wsOwner.getPublicId(), workspace.getPublicId(), wsAdmin.getPublicId(), projectId);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicId(
					projectId, wsAdmin.getPublicId())).isFalse();
		}

		@Test
		@DisplayName("ADMIN이 MEMBER를 강퇴할 수 있다")
		void adminCanKickMember() {
			projectMemberService.kickMember(wsAdmin.getPublicId(), workspace.getPublicId(), wsMember.getPublicId(), projectId);

			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicId(
					projectId, wsMember.getPublicId())).isFalse();
		}

		@Test
		@DisplayName("ADMIN이 리더를 강퇴 불가 → INSUFFICIENT_PERMISSION")
		void adminCannotKickLeader() {
			assertThatThrownBy(() ->
					projectMemberService.kickMember(wsAdmin.getPublicId(), workspace.getPublicId(), wsOwner.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("ADMIN이 다른 ADMIN을 강퇴 불가 → INSUFFICIENT_PERMISSION")
		void adminCannotKickAdmin() {
			User wsAdmin2 = saveUser("admin2@test.com", "관리자2");
			workspaceMemberRepository.saveAndFlush(WorkspaceMember.createGeneralMember(wsAdmin2, workspace, "부팀장"));

			AddMemberRequest addMemberRequest = new AddMemberRequest(wsAdmin2.getPublicId(), ProjectRole.ADMIN, null);
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest);

			assertThatThrownBy(() ->
					projectMemberService.kickMember(wsAdmin.getPublicId(), workspace.getPublicId(), wsAdmin2.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("MEMBER가 강퇴를 시도하면 INSUFFICIENT_PERMISSION")
		void memberCannotKick() {
			assertThatThrownBy(() ->
					projectMemberService.kickMember(wsMember.getPublicId(), workspace.getPublicId(), wsAdmin.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("본인을 강퇴하려 하면 USE_SELF_DELETE_API")
		void selfKick() {
			assertThatThrownBy(() ->
					projectMemberService.kickMember(wsOwner.getPublicId(), workspace.getPublicId(), wsOwner.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(USE_SELF_DELETE_API);
		}

		@Test
		@DisplayName("프로젝트에 없는 사용자를 강퇴하려 하면 PROJECT_MEMBER_NOT_FOUND")
		void targetNotInProject() {
			assertThatThrownBy(() ->
					projectMemberService.kickMember(wsOwner.getPublicId(), workspace.getPublicId(), wsOutsider.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_MEMBER_NOT_FOUND);
		}
	}

	// ===================================================
	// 헬퍼
	private User saveUser(String email, String name) {
		return userRepository.save(User.createDummyUser(name, email, "password1234!", null));
	}

	private Workspace findWorkspaceByName(String name) {
		return workspaceRepository.findAll().stream()
								  .filter(w -> w.getName().equals(name))
								  .findFirst()
								  .orElseThrow();
	}
}
