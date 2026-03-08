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
import xyz.letzcollab.backend.dto.project.*;
import xyz.letzcollab.backend.entity.Project;
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
@DisplayName("ProjectService 통합 테스트")
class ProjectServiceTest {

	@Autowired
	ProjectService projectService;
	@Autowired
	WorkspaceService workspaceService;
	@Autowired
	WorkspaceMemberService workspaceMemberService;
	@Autowired
	private ProjectMemberService projectMemberService;

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

	private User wsOwner;   // 워크스페이스 OWNER
	private User wsAdmin;   // 워크스페이스 ADMIN
	private User wsMember;  // 워크스페이스 MEMBER
	private User stranger;  // 워크스페이스 비소속 외부인

	private Workspace workspace;


	@BeforeEach
	void setUp() {
		wsOwner = saveUser("owner@test.com", "소유자");
		wsAdmin = saveUser("admin@test.com", "관리자");
		wsMember = saveUser("member@test.com", "일반 멤버");
		stranger = saveUser("stranger@test.com", "외부인");

		workspaceService.createWorkspace(wsOwner.getPublicId(), "테스트 워크스페이스", "CTO");
		workspace = findWorkspaceByName("테스트 워크스페이스");

		workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(wsAdmin, workspace, "팀장"));
		workspaceMemberService.updateOtherMember(
				wsOwner.getPublicId(), workspace.getPublicId(), wsAdmin.getPublicId(), null, WorkspaceRole.ADMIN
		);
		workspaceMemberRepository.saveAndFlush(WorkspaceMember.createGeneralMember(wsMember, workspace, "개발자"));
	}


	@Nested
	@DisplayName("프로젝트 생성")
	class CreateProject {

		@Test
		@DisplayName("워크스페이스 OWNER가 프로젝트를 생성하면 DB에 저장되고, 생성자가 ADMIN 멤버로 등록된다")
		void ownerCanCreate() {
			UUID projectId = createProject(wsOwner, "신규 프로젝트", false);

			assertThat(projectRepository.findByPublicIdWithLeader(projectId)).isPresent();
			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicIdAndRole(
					projectId, wsOwner.getPublicId(), ProjectRole.ADMIN
			)).isTrue();
		}

		@Test
		@DisplayName("워크스페이스 ADMIN도 프로젝트를 생성할 수 있다")
		void adminCanCreate() {
			UUID projectId = createProject(wsAdmin, "ADMIN의 프로젝트", false);

			assertThat(projectRepository.findByPublicIdWithLeader(projectId)).isPresent();
			assertThat(projectMemberRepository.existsByProjectPublicIdAndUserPublicIdAndRole(
					projectId, wsAdmin.getPublicId(), ProjectRole.ADMIN
			)).isTrue();
		}

		@Test
		@DisplayName("워크스페이스 MEMBER는 프로젝트 생성 불가 → INSUFFICIENT_PERMISSION")
		void memberCannotCreate() {
			assertThatThrownBy(() -> createProject(wsMember, "MEMBER의 프로젝트", false))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("워크스페이스 비소속 유저는 프로젝트 생성 불가 → WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED")
		void strangerCannotCreate() {
			assertThatThrownBy(() -> createProject(stranger, "외부인의 프로젝트", false))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("같은 워크스페이스에 동일 이름의 프로젝트 생성 시 DUPLICATE_PROJECT_NAME")
		void duplicateNameFails() {
			createProject(wsOwner, "중복 프로젝트", false);

			assertThatThrownBy(() -> createProject(wsOwner, "중복 프로젝트", false))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(DUPLICATE_PROJECT_NAME);
		}
	}


	@Nested
	@DisplayName("프로젝트 목록 조회")
	class GetMyProjects {

		@Test
		@DisplayName("해당 프로젝트에 속한 멤버는 비공개 프로젝트도 조회 목록에서 볼 수 있다")
		void projectMemberSeesPrivateProject() {
			createProject(wsOwner, "비공개 프로젝트", true);

			Page<ProjectResponse> result = projectService.getMyProjects(
					wsOwner.getPublicId(), workspace.getPublicId(),
					new ProjectSearchCond(null, null), PageRequest.of(0, 10)
			);

			assertThat(result.getContent())
					.anyMatch(p -> p.name().equals("비공개 프로젝트"));
		}

		@Test
		@DisplayName("해당 프로젝트에 속하지 않은 멤버는 공개 프로젝트들만 조회 목록에서 볼 수 있다")
		void nonMemberSeesOnlyPublicProject() {
			createProject(wsOwner, "공개 프로젝트", false);
			createProject(wsOwner, "비공개 프로젝트", true);

			// 일반 멤버가 프로젝트 목록 조회
			Page<ProjectResponse> result = projectService.getMyProjects(
					wsMember.getPublicId(), workspace.getPublicId(),
					new ProjectSearchCond(null, null), PageRequest.of(0, 10)
			);

			assertThat(result.getContent()).anyMatch(p -> p.name().equals("공개 프로젝트"));
			assertThat(result.getContent()).noneMatch(p -> p.name().equals("비공개 프로젝트"));
		}

		@Test
		@DisplayName("워크스페이스 비소속 유저가 조회하면 WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED")
		void strangerCannotSeeList() {
			assertThatThrownBy(() -> projectService.getMyProjects(
					stranger.getPublicId(), workspace.getPublicId(),
					new ProjectSearchCond(null, null), PageRequest.of(0, 10)
			))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("프로젝트 상세 조회")
	class GetProjectDetails {

		@Test
		@DisplayName("프로젝트 멤버가 프로젝트 상세 정보를 조회하면 멤버 목록이 포함된다")
		void leaderCanViewDetails() {
			UUID projectId = createProject(wsOwner, "상세 조회 프로젝트", false);

			ProjectDetailsResponse res = projectService.getProjectDetails(
					wsOwner.getPublicId(), workspace.getPublicId(), projectId
			);

			assertThat(res.projectName()).isEqualTo("상세 조회 프로젝트");
			assertThat(res.leader().publicId()).isEqualTo(wsOwner.getPublicId());
			assertThat(res.memberCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("워크스페이스 멤버(비프로젝트 멤버)도 공개 프로젝트 상세 조회 가능")
		void workspaceMemberSeesPublicProject() {
			UUID projectId = createProject(wsOwner, "공개 프로젝트", false);

			ProjectDetailsResponse res = projectService.getProjectDetails(
					wsMember.getPublicId(), workspace.getPublicId(), projectId
			);

			assertThat(res.projectName()).isEqualTo("공개 프로젝트");
		}

		@Test
		@DisplayName("프로젝트에 속하지 않은 멤버는 비공개 프로젝트 상세 조회 불가 → PROJECT_NOT_FOUND_OR_ACCESS_DENIED")
		void nonMemberCannotSeePrivateProject() {
			UUID projectId = createProject(wsOwner, "비공개 프로젝트", true);

			assertThatThrownBy(() -> projectService.getProjectDetails(
					wsMember.getPublicId(), workspace.getPublicId(), projectId
			))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("프로젝트 정보 수정")
	class UpdateProject {

		private UUID projectId;

		@BeforeEach
		void createTestProject() {
			projectId = createProject(wsOwner, "수정 대상 프로젝트", false);
		}

		@Test
		@DisplayName("프로젝트 ADMIN은 이름과 상태를 수정할 수 있다")
		void adminCanUpdateNameAndStatus() {
			UpdateProjectRequest updateReq = new UpdateProjectRequest(
					"수정된 이름", null, ProjectStatus.ON_HOLD, null, null, null
			);
			projectService.updateProject(wsOwner.getPublicId(), projectId, updateReq);

			Project updatedProject = projectRepository.findByPublicIdWithLeader(projectId).orElseThrow();
			assertThat(updatedProject.getName()).isEqualTo("수정된 이름");
			assertThat(updatedProject.getStatus()).isEqualTo(ProjectStatus.ON_HOLD);
		}

		@Test
		@DisplayName("프로젝트 LEADER만 isPrivate 값을 변경할 수 있다")
		void onlyLeaderCanChangeIsPrivate() {
			UpdateProjectRequest updateReq = new UpdateProjectRequest(
					null, null, null, null, null, true
			);
			projectService.updateProject(wsOwner.getPublicId(), projectId, updateReq);

			Project updatedProject = projectRepository.findByPublicIdWithLeader(projectId).orElseThrow();
			assertThat(updatedProject.isPrivate()).isTrue();
		}

		@Test
		@DisplayName("프로젝트 리더가 아닌 ADMIN은 isPrivate 변경 불가 → INSUFFICIENT_PERMISSION")
		void nonLeaderAdminCannotChangeIsPrivate() {
			// given
			AddMemberRequest addMemberRequest = new AddMemberRequest(wsAdmin.getPublicId(), ProjectRole.ADMIN, "PM");
			projectMemberService.addMember(wsOwner.getPublicId(), workspace.getPublicId(), projectId, addMemberRequest);

			// when & then
			UpdateProjectRequest updateReq = new UpdateProjectRequest(
					null, null, null, null, null, true
			);
			assertThatThrownBy(
					() -> projectService.updateProject(wsAdmin.getPublicId(), projectId, updateReq)
			)
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}

		@Test
		@DisplayName("ADMIN이 아닌 프로젝트 멤버가 프로젝트를 수정 시도 -> PROJECT_NOT_FOUND_OR_ACCESS_DENIED")
		void nonAdminCannotUpdate() {
			UpdateProjectRequest updateReq = new UpdateProjectRequest(
					"수정 시도", null, null, null, null, null
			);
			assertThatThrownBy(() -> projectService.updateProject(wsMember.getPublicId(), projectId, updateReq))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}


	@Nested
	@DisplayName("프로젝트 삭제")
	class DeleteProject {

		private UUID projectId;

		@BeforeEach
		void createTestProject() {
			projectId = createProject(wsOwner, "삭제할 프로젝트", false);
		}

		@Test
		@DisplayName("프로젝트 LEADER가 삭제하면 소프트 딜리트 처리된다 (이후 조회 불가)")
		void leaderCanDelete() {
			projectService.deleteProject(wsOwner.getPublicId(), projectId);

			// SQLRestriction("deleted_at IS NULL") 때문에 조회 안 됨
			assertThat(projectRepository.findByPublicIdWithLeader(projectId)).isEmpty();
		}

		@Test
		@DisplayName("프로젝트 LEADER가 아닌 유저가 삭제 시도하면 INSUFFICIENT_PERMISSION")
		void nonLeaderCannotDelete() {
			assertThatThrownBy(() -> projectService.deleteProject(wsMember.getPublicId(), projectId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(INSUFFICIENT_PERMISSION);
		}
	}


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

	private UUID createProject(User leader, String name, boolean isPrivate) {
		return projectService.createProject(leader.getPublicId(), workspace.getPublicId(),
				new CreateProjectRequest(name, null, ProjectStatus.ACTIVE, null, null, isPrivate, null));
	}
}
