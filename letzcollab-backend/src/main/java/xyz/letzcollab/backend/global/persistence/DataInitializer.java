package xyz.letzcollab.backend.global.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;
import xyz.letzcollab.backend.service.ProjectMemberService;
import xyz.letzcollab.backend.service.ProjectService;
import xyz.letzcollab.backend.service.WorkspaceService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final WorkspaceService workspaceService;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final ProjectService projectService;
	private final ProjectMemberService projectMemberService;

	@Value("${users.admin.password}")
	private String adminPassword;

	@Override
	@Transactional
	public void run(String... args) throws Exception {

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
				passwordEncoder.encode(adminPassword),
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
