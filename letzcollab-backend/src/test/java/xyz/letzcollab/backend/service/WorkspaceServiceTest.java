package xyz.letzcollab.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.TestAuditConfig;
import xyz.letzcollab.backend.dto.workspace.WorkspaceDetailsResponse;
import xyz.letzcollab.backend.dto.workspace.WorkspaceResponse;
import xyz.letzcollab.backend.dto.workspace.WorkspaceStatsResponse;
import xyz.letzcollab.backend.entity.*;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestAuditConfig.class)
@DisplayName("WorkspaceService 통합 테스트")
class WorkspaceServiceTest {
	@Autowired
	WorkspaceService workspaceService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	WorkspaceRepository workspaceRepository;

	@Autowired
	WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	TaskRepository taskRepository;

	private User owner;
	private User otherUser;

	@BeforeEach
	void setUp() {
		owner = saveUser("owner@test.com", "소유자");
		otherUser = saveUser("other@test.com", "다른 사용자");
	}

	@Nested
	@DisplayName("워크스페이스 생성")
	class CreateWorkspace {

		@Test
		@DisplayName("반환된 워크스페이스 publicId로 워크스페이스를 조회할 수 있다")
		void returnsValidPublicId() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			assertThat(publicId).isNotNull();
			assertThat(workspaceRepository.findWorkspaceByPublicIdWithOwner(publicId)).isPresent();
		}

		@Test
		@DisplayName("생성 시 OWNER 멤버가 자동 추가되고 역할·직책이 올바르게 저장된다")
		void createsWorkspaceWithOwnerMember() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			Workspace saved = workspaceRepository.findWorkspaceWithAllMembers(publicId).orElseThrow();

			assertThat(saved.getOwner().getPublicId()).isEqualTo(owner.getPublicId());
			assertThat(saved.getMembers()).hasSize(1);
			assertThat(saved.getMembers().getFirst().getRole()).isEqualTo(WorkspaceRole.OWNER);
			assertThat(saved.getMembers().getFirst().getPosition()).isEqualTo("CTO");
		}

		@Test
		@DisplayName("존재하지 않는 유저로 생성하면 USER_NOT_FOUND")
		void userNotFound() {
			assertThatThrownBy(() ->
					workspaceService.createWorkspace(UUID.randomUUID(), "우아한동네 개발팀", "CTO"))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(USER_NOT_FOUND);
		}

		@Test
		@DisplayName("같은 소유자가 동일 이름으로 생성 시도하면 DUPLICATE_WORKSPACE_NAME")
		void sameOwnerDuplicateNameFails() {
			workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			assertThatThrownBy(() ->
					workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CEO"))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(DUPLICATE_WORKSPACE_NAME);
		}

		@Test
		@DisplayName("다른 소유자는 동일 이름으로 생성할 수 있다")
		void differentOwnerSameNameAllowed() {
			UUID first  = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");
			UUID second = workspaceService.createWorkspace(otherUser.getPublicId(), "우아한동네 개발팀", "CEO");

			assertThat(first).isNotEqualTo(second);
			assertThat(workspaceRepository.findWorkspaceByPublicIdWithOwner(first)).isPresent();
			assertThat(workspaceRepository.findWorkspaceByPublicIdWithOwner(second)).isPresent();
		}

		@Test
		@DisplayName("삭제 후 같은 이름으로 재생성할 수 있다")
		void canRecreateAfterSoftDelete() {
			// given
			UUID first = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			// when
			workspaceService.deleteWorkspace(owner.getPublicId(), first);

			// then
			// softDelete시 name이 바뀌므로 같은 이름으로 재생성 가능
			UUID second = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			assertThat(second).isNotEqualTo(first);
			assertThat(workspaceRepository.findWorkspaceByPublicIdWithOwner(second)).isPresent();
		}

		@Test
		@DisplayName("position이 null이면 null로 저장된다")
		void nullPositionSavedAsNull() {
			workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", null);

			WorkspaceMember member = workspaceMemberRepository.findAllWithWorkspace(owner.getPublicId())
															  .stream()
															  .findFirst()
															  .orElseThrow();

			assertThat(member.getPosition()).isNull();
		}
	}

	@Nested
	@DisplayName("내 워크스페이스 목록 조회")
	class GetMyWorkspaces {

		@Test
		@DisplayName("본인이 속한 워크스페이스 목록을 반환한다")
		void returnsMemberWorkspaces() {
			workspaceService.createWorkspace(owner.getPublicId(), "나만의 비밀 연구소", "CTO");
			workspaceService.createWorkspace(owner.getPublicId(), "삼성전자 가전사업부", "CEO");

			List<WorkspaceResponse> result = workspaceService.getMyWorkspaces(owner.getPublicId());

			assertThat(result).hasSize(2);
		}

		@Test
		@DisplayName("속한 워크스페이스가 없으면 빈 목록 반환")
		void returnsEmptyWhenNone() {
			List<WorkspaceResponse> result = workspaceService.getMyWorkspaces(otherUser.getPublicId());
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("워크스페이스 목록 조회 응답의 publicId는 워크스페이스 publicId다")
		void responsePublicIdIsWorkspacePublicId() {
			UUID workspacePublicId = workspaceService.createWorkspace(owner.getPublicId(), "나만의 비밀 연구소", "CTO");

			WorkspaceResponse response = workspaceService.getMyWorkspaces(owner.getPublicId()).getFirst();

			assertThat(response.publicId()).isEqualTo(workspacePublicId);
		}
	}

	@Nested
	@DisplayName("특정 워크스페이스에 대한 상세정보 조회")
	class GetWorkspaceDetails {

		@Test
		@DisplayName("소유자가 상세 조회 시 isOwner=true, 전체 멤버 목록 포함")
		void ownerSeesDetails() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			WorkspaceDetailsResponse response = workspaceService.getWorkspaceDetails(owner.getPublicId(), publicId);

			assertThat(response.isOwner()).isTrue();
			assertThat(response.memberCount()).isEqualTo(1);
			assertThat(response.myPosition()).isEqualTo("CTO");
			assertThat(response.owner().publicId()).isEqualTo(owner.getPublicId());
		}

		@Test
		@DisplayName("멤버가 아닌 유저는 WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED 오류코드 반환")
		void nonMemberDenied() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			assertThatThrownBy(() ->
					workspaceService.getWorkspaceDetails(otherUser.getPublicId(), publicId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}

		@Test
		@DisplayName("존재하지 않는 워크스페이스 조회 시 WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED")
		void workspaceNotFound() {
			assertThatThrownBy(() ->
					workspaceService.getWorkspaceDetails(owner.getPublicId(), UUID.randomUUID()))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}

	@Nested
	@DisplayName("워크스페이스 수정")
	class UpdateWorkspace {

		@Test
		@DisplayName("소유자가 워크스페이스 이름을 수정하면 DB에 반영된다")
		void ownerCanRename() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "LetzCollab 개발 팀", "CTO");

			workspaceService.updateWorkspace(owner.getPublicId(), publicId, "LetzCollab 백엔드 개발 팀");

			Workspace updated = workspaceRepository.findWorkspaceByPublicIdWithOwner(publicId).orElseThrow();
			assertThat(updated.getName()).isEqualTo("LetzCollab 백엔드 개발 팀");
		}

		@Test
		@DisplayName("소유자가 아닌 유저가 수정을 시도하면 WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED 오류코드 반환")
		void nonOwnerDenied() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			assertThatThrownBy(() ->
					workspaceService.updateWorkspace(otherUser.getPublicId(), publicId, "해커들의 모임"))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}

	@Nested
	@DisplayName("워크스페이스 삭제")
	class DeleteWorkspace {

		@Test
		@DisplayName("소유자가 삭제하면 softDelete 처리되어 조회되지 않고, 해당 워크스페이스의 이름이 변경된다")
		void ownerCanDelete() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			workspaceService.deleteWorkspace(owner.getPublicId(), publicId);

			// Workspace 엔티티에 붙은 @SQLRestriction("deleted_at IS NULL") 으로 인해 조회 불가
			assertThat(workspaceRepository.findWorkspaceByPublicIdWithOwner(publicId)).isEmpty();
			// name이 변경되었기에 삭제한 워크스페이스 이름으로 새로운 워크스페이스를 생성할 수 있음
			assertThatNoException().isThrownBy(() ->
					workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO"));
		}

		@Test
		@DisplayName("소유자가 아닌 사용자가 삭제 시도하면 WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED 오류코드 반환")
		void nonOwnerDenied() {
			UUID publicId = workspaceService.createWorkspace(owner.getPublicId(), "우아한동네 개발팀", "CTO");

			assertThatThrownBy(() ->
					workspaceService.deleteWorkspace(otherUser.getPublicId(), publicId))
					.isInstanceOf(CustomException.class)
					.extracting(e -> ((CustomException) e).getErrorCode())
					.isEqualTo(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}

	@Nested
	@DisplayName("워크스페이스 통계 조회")
	class GetWorkspaceStats {
		@Test
		@DisplayName("워크스페이스에 멤버 1명, 프로젝트 2개, 각 프로젝트에 업무 3개 있을 때 올바른 통계를 반환한다")
		void getsCorrectStats() {
			// given
			Workspace workspace = Workspace.createWorkspace("우아한동네 개발팀", owner, "CTO");
			workspaceRepository.save(workspace);
			workspaceMemberRepository.save(WorkspaceMember.createGeneralMember(otherUser, workspace, "디자이너"));

			for (int i = 0; i < 2; i++) {
				Project project = Project.createProject(
						workspace, "프로젝트" + i, "", ProjectStatus.ACTIVE, LocalDate.now(),
						LocalDate.now().plusDays(i), false, owner, "CTO"
				);
				projectRepository.save(project);

				for (int j = 0; j < 3; j++) {
					// TODO 상태인 업무를 3개 생성
					Task task = Task.createTask(
							project, "업무" + i, "", otherUser, TaskPriority.values()[j], null,
							owner, LocalDate.now().plusDays(30)
					);
					taskRepository.save(task);
				}
			}

			// when
			WorkspaceStatsResponse stats = workspaceService.getStats(owner.getPublicId(), workspace.getPublicId());

			// then
			assertThat(stats.projects().total()).isEqualTo(2);
			assertThat(stats.projects().planned()).isEqualTo(0);
			assertThat(stats.projects().active()).isEqualTo(2);
			assertThat(stats.projects().onHold()).isEqualTo(0);
			assertThat(stats.projects().completed()).isEqualTo(0);
			assertThat(stats.projects().archived()).isEqualTo(0);

			assertThat(stats.tasks().total()).isEqualTo(6);
			assertThat(stats.tasks().todo()).isEqualTo(6);
			assertThat(stats.tasks().inProgress()).isEqualTo(0);
			assertThat(stats.tasks().inReview()).isEqualTo(0);
			assertThat(stats.tasks().done()).isEqualTo(0);
			assertThat(stats.tasks().cancelled()).isEqualTo(0);
			assertThat(stats.tasks().overdue()).isEqualTo(0);
			assertThat(stats.tasks().completionRate()).isEqualTo(0);

			assertThat(stats.totalMembers()).isEqualTo(2);
		}
	}


	// 헬퍼 메소드
	private User saveUser(String email, String name) {
		return userRepository.save(
				User.createDummyUser(name, email, "pwd1234!?", null)
		);
	}
}