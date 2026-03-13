package xyz.letzcollab.backend.global.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.CreateProjectRequest;
import xyz.letzcollab.backend.dto.task.CreateTaskRequest;
import xyz.letzcollab.backend.dto.task.UpdateTaskRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.*;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;
import xyz.letzcollab.backend.service.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
	private final PasswordEncoder passwordEncoder;

	private final WorkspaceService workspaceService;
	private final ProjectService projectService;
	private final ProjectMemberService projectMemberService;
	private final TaskService taskService;
	private final TaskCommentService taskCommentService;

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

		if (userRepository.existsByEmail("honggildong@naver.com")) return;

		// 사용자
		User dummyUser = User.createDummyUser(
				"홍길동",
				"honggildong@naver.com",
				passwordEncoder.encode("123456"),
				"010-2345-4321"
		);

		User dummyUser2 = User.createDummyUser(
				"peter",
				"peter8790@naver.com",
				passwordEncoder.encode("123456"),
				"010-8765-4321"
		);

		User adminUser = User.createAdminUser(
				"admin",
				"admin@letzcollab.xyz",
				passwordEncoder.encode("123456"),
				"010-1234-5678"
		);

		userRepository.save(dummyUser);
		userRepository.save(dummyUser2);
		userRepository.save(adminUser);

		// AUDIT 정보를 채울 수 있도록 시큐리티 콘텍스트에 임의의 값 넣기
		setSecurityContext();

		// 워크스페이스
		UUID workspaceId = workspaceService.createWorkspace(dummyUser.getPublicId(), "우아한동네 개발팀", "CTO");
		Workspace workspace = workspaceRepository.findWorkspaceByPublicIdWithOwner(workspaceId).orElseThrow();

		WorkspaceMember generalMember = WorkspaceMember.createGeneralMember(dummyUser2, workspace, "백엔드 개발자");
		WorkspaceMember adminMember = WorkspaceMember.createGeneralMember(adminUser, workspace, "PM");
		adminMember.updateInfo(null, WorkspaceRole.ADMIN);

		workspaceMemberRepository.save(generalMember);
		workspaceMemberRepository.save(adminMember);

		// 프로젝트
		CreateProjectRequest createProjectRequest = new CreateProjectRequest(
				"데이터베이스 최적화", "성능 개선 및 쿼리 최적화", ProjectStatus.ACTIVE,
				LocalDate.now(), LocalDate.now().plusDays(10), false, "CTO"
		);
		UUID projectId = projectService.createProject(dummyUser.getPublicId(), workspaceId, createProjectRequest);


		projectMemberService.addMember(
				dummyUser.getPublicId(), workspaceId, projectId,
				new AddMemberRequest(dummyUser2.getPublicId(), ProjectRole.MEMBER, null)
		);

		projectMemberService.addMember(
				dummyUser.getPublicId(), workspaceId, projectId,
				new AddMemberRequest(adminUser.getPublicId(), ProjectRole.ADMIN, null)
		);

		// 업무
		UUID task1Id = taskService.createTask(
				dummyUser.getPublicId(), projectId,
				new CreateTaskRequest("쿼리 실행 계획 분석", "느린 쿼리 EXPLAIN 분석 및 인덱스 보완",
						dummyUser2.getPublicId(), TaskPriority.URGENT, LocalDate.now().plusDays(3))
		);

		UUID task2Id = taskService.createTask(
				dummyUser.getPublicId(), projectId,
				new CreateTaskRequest("N+1 문제 해결", "JPA Fetch Join 적용",
						dummyUser.getPublicId(), TaskPriority.HIGH, LocalDate.now().plusDays(5))
		);

		taskService.createTask(
				dummyUser.getPublicId(), projectId,
				new CreateTaskRequest("커넥션 풀 튜닝", "HikariCP 설정 최적화",
						adminUser.getPublicId(), TaskPriority.MEDIUM, LocalDate.now().plusDays(7))
		);

		taskService.updateTask(dummyUser.getPublicId(), projectId, task1Id,
				new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));

		taskService.updateTask(dummyUser.getPublicId(), projectId, task2Id,
				new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));

		// 하위 업무
		taskService.createSubTask(
				dummyUser.getPublicId(), projectId, task1Id,
				new CreateTaskRequest("인덱스 추가 적용", null,
						dummyUser2.getPublicId(), TaskPriority.HIGH, LocalDate.now().plusDays(2))
		);

		taskService.createSubTask(
				dummyUser.getPublicId(), projectId, task1Id,
				new CreateTaskRequest("쿼리 리팩토링", null,
						dummyUser.getPublicId(), TaskPriority.MEDIUM, LocalDate.now().plusDays(3))
		);

		taskCommentService.createComment(
				dummyUser2.getPublicId(), projectId, task1Id,
				"EXPLAIN 결과 보니까 full scan 나오는 쿼리 3개 있어요. 인덱스 추가하면 될 것 같습니다.", null
		);

		taskCommentService.createComment(
				dummyUser.getPublicId(), projectId, task1Id,
				"어떤 컬럼에 인덱스 추가하면 될까요?", null
		);

		Long secondCommentId = taskCommentService.getComments(dummyUser.getPublicId(), projectId, task1Id)
												 .get(1).commentId();

		taskCommentService.createComment(
				dummyUser2.getPublicId(), projectId, task1Id,
				"user_id, created_at 복합 인덱스 추가하면 될 것 같아요.", secondCommentId
		);

		taskCommentService.createComment(
				dummyUser.getPublicId(), projectId, task2Id,
				"Fetch Join 적용 완료했습니다. 쿼리 수 87개 → 3개로 줄었어요.", null
		);

		taskCommentService.createComment(
				adminUser.getPublicId(), projectId, task2Id,
				"수고하셨습니다! 코드 리뷰 확인 후 merge할게요.", null
		);
	}

	private void setSecurityContext() {
		CustomUserDetails userDetails = new CustomUserDetails(
				"",
				UUID.randomUUID(),
				"",
				"",
				UserRole.ADMIN,
				null
		);

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
				userDetails, "", List.of(new SimpleGrantedAuthority(UserRole.ADMIN.getAuthority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authenticationToken);
	}
}
