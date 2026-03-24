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
	public void run(String... args) {

		if (userRepository.existsByEmail("honggildong@naver.com")) return;

		// ──────────────────────────────────────────
		// 1. 사용자 생성
		// ──────────────────────────────────────────
		// 포트폴리오 체험 계정
		User hong = User.createDummyUser("홍길동", "honggildong@naver.com", passwordEncoder.encode("123456"), "010-2345-4321");

		// 개발팀 동료들
		User kim = User.createDummyUser("김민준", "kiminjun@gmail.com", passwordEncoder.encode("123456"), "010-1111-2222");
		User lee = User.createDummyUser("이서연", "leesyeon@gmail.com", passwordEncoder.encode("123456"), "010-3333-4444");
		User park = User.createDummyUser("박지호", "parkjiho@gmail.com", passwordEncoder.encode("123456"), "010-5555-6666");

		// 디자인/기획팀
		User choi = User.createDummyUser("최예린", "choiyerin@naver.com", passwordEncoder.encode("123456"), "010-7777-8888");
		User jung = User.createDummyUser("정다은", "jungdaeun@naver.com", passwordEncoder.encode("123456"), "010-9999-0000");

		User admin = User.createAdminUser("admin", "admin@letzcollab.xyz", passwordEncoder.encode("123456"), "010-1234-5678");

		userRepository.saveAll(List.of(hong, kim, lee, park, choi, jung, admin));

		setSecurityContext();

		// ──────────────────────────────────────────
		// 2. 워크스페이스 A — "우아한동네 개발팀"
		//    홍길동: OWNER / 김민준: ADMIN / 이서연,박지호: MEMBER
		// ──────────────────────────────────────────
		UUID wsDevId = workspaceService.createWorkspace(hong.getPublicId(), "우아한동네 개발팀", "테크 리드");
		Workspace wsDev = workspaceRepository.findWorkspaceByPublicIdWithOwner(wsDevId).orElseThrow();

		WorkspaceMember kimMember = WorkspaceMember.createGeneralMember(kim, wsDev, "백엔드 개발자");
		WorkspaceMember leeMember = WorkspaceMember.createGeneralMember(lee, wsDev, "프론트엔드 개발자");
		WorkspaceMember parkMember = WorkspaceMember.createGeneralMember(park, wsDev, "DevOps 엔지니어");
		kimMember.updateInfo(null, WorkspaceRole.ADMIN);

		workspaceMemberRepository.saveAll(List.of(kimMember, leeMember, parkMember));

		// ── 프로젝트 1: 백엔드 API 리팩토링 (홍길동 리더, 진행 중)
		UUID proj1Id = projectService.createProject(hong.getPublicId(), wsDevId,
				new CreateProjectRequest("백엔드 API 리팩토링", "레거시 REST API를 개선하고 성능을 최적화합니다.",
						ProjectStatus.ACTIVE, LocalDate.now().minusDays(20), LocalDate.now()
																					  .plusDays(40), false, "테크 리드"));

		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj1Id, new AddMemberRequest(kim.getPublicId(), ProjectRole.ADMIN, "백엔드 개발자"));
		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj1Id, new AddMemberRequest(lee.getPublicId(), ProjectRole.MEMBER, "프론트엔드 개발자"));
		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj1Id, new AddMemberRequest(park.getPublicId(), ProjectRole.VIEWER, "DevOps 엔지니어"));

		// 업무
		UUID t1_1 = taskService.createTask(hong.getPublicId(), proj1Id,
				new CreateTaskRequest("쿼리 실행 계획 분석", "느린 쿼리 EXPLAIN 분석 및 인덱스 보완", hong.getPublicId(), TaskPriority.URGENT, LocalDate.now()
																																	.plusDays(3)));
		UUID t1_2 = taskService.createTask(hong.getPublicId(), proj1Id,
				new CreateTaskRequest("N+1 문제 해결", "JPA Fetch Join 적용으로 N+1 쿼리 제거", kim.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																																   .plusDays(7)));
		UUID t1_3 = taskService.createTask(hong.getPublicId(), proj1Id,
				new CreateTaskRequest("API 응답 포맷 통일", "ApiResponse 공통 래퍼 적용", lee.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																															   .plusDays(14)));
		UUID t1_4 = taskService.createTask(kim.getPublicId(), proj1Id,
				new CreateTaskRequest("커넥션 풀 튜닝", "HikariCP 설정 최적화", hong.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																													   .plusDays(10)));
		UUID t1_5 = taskService.createTask(kim.getPublicId(), proj1Id,
				new CreateTaskRequest("인증 미들웨어 리팩토링", "JWT 필터 코드 개선 및 테스트 작성", kim.getPublicId(), TaskPriority.LOW, LocalDate.now()
																															 .plusDays(20)));

		// 상태 업데이트
		taskService.updateTask(hong.getPublicId(), proj1Id, t1_1, new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));
		taskService.updateTask(kim.getPublicId(), proj1Id, t1_2, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));
		taskService.updateTask(lee.getPublicId(), proj1Id, t1_3, new UpdateTaskRequest(null, null, TaskStatus.IN_REVIEW, null, null, null));

		// 하위 업무
		taskService.createSubTask(hong.getPublicId(), proj1Id, t1_1,
				new CreateTaskRequest("인덱스 추가 적용", null, hong.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																										 .plusDays(2)));
		taskService.createSubTask(hong.getPublicId(), proj1Id, t1_1,
				new CreateTaskRequest("쿼리 리팩토링", null, kim.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																										.plusDays(3)));

		// 댓글
		taskCommentService.createComment(kim.getPublicId(), proj1Id, t1_1,
				"EXPLAIN 결과 full scan 나오는 쿼리 3개 확인했어요. user_id, created_at 복합 인덱스 추가하면 될 것 같습니다.", null);
		taskCommentService.createComment(hong.getPublicId(), proj1Id, t1_1,
				"확인했습니다. 인덱스 추가 후 재측정 부탁드려요.", null);

		List<Long> t1_1Comments = taskCommentService.getComments(hong.getPublicId(), proj1Id, t1_1)
													.stream().map(c -> c.commentId()).toList();
		taskCommentService.createComment(kim.getPublicId(), proj1Id, t1_1,
				"넵! 적용 후 쿼리 시간 87ms → 12ms로 줄었습니다 🎉", t1_1Comments.get(1));

		taskCommentService.createComment(hong.getPublicId(), proj1Id, t1_2,
				"Fetch Join 적용 완료했습니다. 쿼리 수 87개 → 3개로 줄었어요.", null);
		taskCommentService.createComment(kim.getPublicId(), proj1Id, t1_2,
				"수고하셨습니다! 코드 리뷰 확인 후 merge할게요.", null);

		taskCommentService.createComment(lee.getPublicId(), proj1Id, t1_3,
				"ApiResponse 래퍼 PR 올렸습니다. 검토 부탁드립니다.", null);
		taskCommentService.createComment(hong.getPublicId(), proj1Id, t1_3,
				"전반적으로 좋은데 에러 응답 형식에서 errorCode 필드 누락된 케이스 있습니다.", null);

		// ── 프로젝트 2: 프론트엔드 대시보드 개발 (홍길동 리더, 진행 중)
		UUID proj2Id = projectService.createProject(hong.getPublicId(), wsDevId,
				new CreateProjectRequest("프론트엔드 대시보드 개발", "React 기반 협업 관리 대시보드를 구현합니다.",
						ProjectStatus.ACTIVE, LocalDate.now().minusDays(10), LocalDate.now()
																					  .plusDays(30), false, "테크 리드"));

		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj2Id, new AddMemberRequest(lee.getPublicId(), ProjectRole.ADMIN, "프론트엔드 리드"));
		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj2Id, new AddMemberRequest(kim.getPublicId(), ProjectRole.MEMBER, "백엔드 연동"));

		UUID t2_1 = taskService.createTask(hong.getPublicId(), proj2Id,
				new CreateTaskRequest("워크스페이스 선택 UI", "워크스페이스 셀렉터 컴포넌트 구현", lee.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																														   .plusDays(5)));
		UUID t2_2 = taskService.createTask(hong.getPublicId(), proj2Id,
				new CreateTaskRequest("프로젝트 카드 컴포넌트", "ProjectCard 컴포넌트 구현 및 스타일링", lee.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																																	 .plusDays(8)));
		UUID t2_3 = taskService.createTask(lee.getPublicId(), proj2Id,
				new CreateTaskRequest("axios 인터셉터 설정", "JWT 만료 처리 인터셉터 추가", hong.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																															.plusDays(2)));
		UUID t2_4 = taskService.createTask(lee.getPublicId(), proj2Id,
				new CreateTaskRequest("TanStack Query 연동", "useQuery로 API 데이터 패칭 구현", lee.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																																	   .plusDays(12)));

		taskService.updateTask(lee.getPublicId(), proj2Id, t2_1, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));
		taskService.updateTask(lee.getPublicId(), proj2Id, t2_2, new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));
		taskService.updateTask(hong.getPublicId(), proj2Id, t2_3, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));

		taskCommentService.createComment(lee.getPublicId(), proj2Id, t2_1,
				"워크스페이스 셀렉터 완료했습니다. 탭 선택 시 lazy loading으로 프로젝트 목록 불러오도록 구현했어요.", null);
		taskCommentService.createComment(hong.getPublicId(), proj2Id, t2_1,
				"깔끔하게 잘 됐네요! 머지합니다 👍", null);
		taskCommentService.createComment(hong.getPublicId(), proj2Id, t2_3,
				"A001, A003 에러코드 모두 인터셉터에서 처리하도록 추가했습니다.", null);

		// ── 프로젝트 3: 인프라 개선 (박지호 리더, 기획 중)
		UUID proj3Id = projectService.createProject(hong.getPublicId(), wsDevId,
				new CreateProjectRequest("인프라 개선", "Docker 컨테이너화 및 CI/CD 파이프라인 구축",
						ProjectStatus.PLANNED, LocalDate.now().plusDays(30), LocalDate.now()
																					  .plusDays(90), true, "테크 리드"));

		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj3Id, new AddMemberRequest(park.getPublicId(), ProjectRole.ADMIN, "DevOps 리드"));
		projectMemberService.addMember(hong.getPublicId(), wsDevId, proj3Id, new AddMemberRequest(kim.getPublicId(), ProjectRole.MEMBER, "백엔드 개발자"));

		UUID t3_1 = taskService.createTask(hong.getPublicId(), proj3Id,
				new CreateTaskRequest("Dockerfile 작성", "백엔드/프론트엔드 Dockerfile 작성", park.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																																  .plusDays(35)));
		UUID t3_2 = taskService.createTask(hong.getPublicId(), proj3Id,
				new CreateTaskRequest("GitHub Actions CI 설정", "PR 빌드 및 테스트 자동화", park.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																																   .plusDays(50)));

		taskCommentService.createComment(park.getPublicId(), proj3Id, t3_1,
				"multi-stage build로 이미지 크기 최소화할 예정입니다.", null);
		taskCommentService.createComment(hong.getPublicId(), proj3Id, t3_1,
				"좋아요! 빌드 캐시도 활용해주세요.", null);

		// ──────────────────────────────────────────
		// 3. 워크스페이스 B — "스타트업 디자인스튜디오"
		//    최예린: OWNER / 홍길동: ADMIN / 정다은: MEMBER
		// ──────────────────────────────────────────
		UUID wsDesignId = workspaceService.createWorkspace(choi.getPublicId(), "스타트업 디자인스튜디오", "디자인 디렉터");
		Workspace wsDesign = workspaceRepository.findWorkspaceByPublicIdWithOwner(wsDesignId).orElseThrow();

		WorkspaceMember hongDesignMember = WorkspaceMember.createGeneralMember(hong, wsDesign, "프론트엔드 개발자");
		WorkspaceMember jungMember = WorkspaceMember.createGeneralMember(jung, wsDesign, "UI/UX 디자이너");
		hongDesignMember.updateInfo(null, WorkspaceRole.ADMIN);

		workspaceMemberRepository.saveAll(List.of(hongDesignMember, jungMember));

		// ── 프로젝트 4: 서비스 랜딩페이지 리뉴얼 (최예린 리더, 진행 중)
		UUID proj4Id = projectService.createProject(choi.getPublicId(), wsDesignId,
				new CreateProjectRequest("랜딩페이지 리뉴얼", "브랜드 아이덴티티를 반영한 랜딩페이지 리디자인 및 개발",
						ProjectStatus.ACTIVE, LocalDate.now().minusDays(5), LocalDate.now()
																					 .plusDays(25), false, "디자인 디렉터"));

		projectMemberService.addMember(choi.getPublicId(), wsDesignId, proj4Id, new AddMemberRequest(hong.getPublicId(), ProjectRole.ADMIN, "프론트엔드 개발자"));
		projectMemberService.addMember(choi.getPublicId(), wsDesignId, proj4Id, new AddMemberRequest(jung.getPublicId(), ProjectRole.MEMBER, "UI/UX 디자이너"));

		UUID t4_1 = taskService.createTask(choi.getPublicId(), proj4Id,
				new CreateTaskRequest("히어로 섹션 디자인", "메인 히어로 배너 시안 3종 제작", jung.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																														  .plusDays(4)));
		UUID t4_2 = taskService.createTask(choi.getPublicId(), proj4Id,
				new CreateTaskRequest("히어로 섹션 개발", "디자인 시안 기반 반응형 구현", hong.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																													   .plusDays(10)));
		UUID t4_3 = taskService.createTask(hong.getPublicId(), proj4Id,
				new CreateTaskRequest("애니메이션 효과 적용", "스크롤 트리거 fade-in 애니메이션", hong.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																																.plusDays(15)));
		UUID t4_4 = taskService.createTask(choi.getPublicId(), proj4Id,
				new CreateTaskRequest("모바일 반응형 검수", "iPhone/Android 주요 기기 QA", jung.getPublicId(), TaskPriority.URGENT, LocalDate.now()
																																 .plusDays(20)));

		taskService.updateTask(choi.getPublicId(), proj4Id, t4_1, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));
		taskService.updateTask(hong.getPublicId(), proj4Id, t4_2, new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null));

		// 하위 업무
		taskService.createSubTask(choi.getPublicId(), proj4Id, t4_2,
				new CreateTaskRequest("Figma 토큰 CSS 변수 변환", null, hong.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																													.plusDays(8)));
		taskService.createSubTask(choi.getPublicId(), proj4Id, t4_2,
				new CreateTaskRequest("크로스브라우저 호환성 확인", null, hong.getPublicId(), TaskPriority.LOW, LocalDate.now()
																											 .plusDays(9)));

		taskCommentService.createComment(jung.getPublicId(), proj4Id, t4_1,
				"시안 3종 완성했습니다! Figma 링크 공유드릴게요.", null);
		taskCommentService.createComment(choi.getPublicId(), proj4Id, t4_1,
				"2번 시안으로 진행하겠습니다. 수고하셨어요!", null);
		taskCommentService.createComment(hong.getPublicId(), proj4Id, t4_2,
				"반응형 구현 중입니다. breakpoint는 768px, 1280px 기준으로 잡을게요.", null);
		taskCommentService.createComment(choi.getPublicId(), proj4Id, t4_2,
				"혹시 375px(iPhone SE)도 대응 가능한지 확인해주실 수 있어요?", null);

		List<Long> t4_2Comments = taskCommentService.getComments(choi.getPublicId(), proj4Id, t4_2)
													.stream().map(c -> c.commentId()).toList();
		taskCommentService.createComment(hong.getPublicId(), proj4Id, t4_2,
				"네, 375px도 대응했습니다!", t4_2Comments.get(t4_2Comments.size() - 1));

		// ── 프로젝트 5: 디자인 시스템 구축 (최예린 리더, 완료)
		UUID proj5Id = projectService.createProject(choi.getPublicId(), wsDesignId,
				new CreateProjectRequest("디자인 시스템 구축", "컴포넌트 라이브러리 및 스타일 가이드 제작",
						ProjectStatus.COMPLETED, LocalDate.now().minusDays(60), LocalDate.now()
																						 .minusDays(5), false, "디자인 디렉터"));

		projectMemberService.addMember(choi.getPublicId(), wsDesignId, proj5Id, new AddMemberRequest(jung.getPublicId(), ProjectRole.MEMBER, "UI/UX 디자이너"));
		projectMemberService.addMember(choi.getPublicId(), wsDesignId, proj5Id, new AddMemberRequest(hong.getPublicId(), ProjectRole.VIEWER, "검토자"));

		UUID t5_1 = taskService.createTask(choi.getPublicId(), proj5Id,
				new CreateTaskRequest("컬러 토큰 정의", "브랜드 컬러 팔레트 및 시맨틱 토큰 정의", jung.getPublicId(), TaskPriority.HIGH, LocalDate.now()
																															.minusDays(40)));
		UUID t5_2 = taskService.createTask(choi.getPublicId(), proj5Id,
				new CreateTaskRequest("타이포그래피 가이드", "폰트 스케일 및 사용 규칙 문서화", choi.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																															.minusDays(30)));
		UUID t5_3 = taskService.createTask(choi.getPublicId(), proj5Id,
				new CreateTaskRequest("버튼 컴포넌트 정의", "상태별(default/hover/disabled) 버튼 스펙 정의", jung.getPublicId(), TaskPriority.MEDIUM, LocalDate.now()
																																			  .minusDays(20)));

		taskService.updateTask(choi.getPublicId(), proj5Id, t5_1, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));
		taskService.updateTask(choi.getPublicId(), proj5Id, t5_2, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));
		taskService.updateTask(choi.getPublicId(), proj5Id, t5_3, new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null));

		taskCommentService.createComment(jung.getPublicId(), proj5Id, t5_1,
				"primary, secondary, semantic 3가지 카테고리로 나눠서 정리했습니다.", null);
		taskCommentService.createComment(choi.getPublicId(), proj5Id, t5_1,
				"완벽해요! Figma variables에도 반영 완료했습니다.", null);
		taskCommentService.createComment(choi.getPublicId(), proj5Id, t5_2,
				"Pretendard 기반으로 12/14/16/18/24/32/48px 스케일 확정했습니다.", null);
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
