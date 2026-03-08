package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.project.*;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.ProjectMemberRepository;
import xyz.letzcollab.backend.repository.ProjectRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;

import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectService {
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	public UUID createProject(UUID userPublicId, UUID workspacePublicId, CreateProjectRequest req) {
		WorkspaceMember requester = workspaceMemberRepository.findMemberWithWorkspaceAndUser(workspacePublicId, userPublicId)
															 .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));
		if (!requester.canCreateProject()) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}

		User leader = requester.getUser();
		Workspace workspace = requester.getWorkspace();

		if (projectRepository.existsByWorkspaceAndName(workspace, req.name())) {
			throw new CustomException(DUPLICATE_PROJECT_NAME);
		}

		String position = req.position() == null ? requester.getPosition() : req.position();

		Project project = Project.createProject(
				workspace, req.name(), req.description(), req.status(), req.startDate(), req.endDate(), req.isPrivate(),
				leader, position
		);

		projectRepository.save(project);
		log.info("프로젝트 생성 - projectName={}, leaderId={}, workspaceId={}",
				req.name(), userPublicId, workspacePublicId);

		return project.getPublicId();
	}

	@Transactional(readOnly = true)
	public Page<ProjectResponse> getMyProjects(UUID userPublicId, UUID workspacePublicId, ProjectSearchCond cond, Pageable pageable) {
		if (!workspaceMemberRepository.existsByWorkspacePublicIdAndUserPublicId(workspacePublicId, userPublicId)) {
			throw new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}

		return projectRepository.findProjectsByCondition(userPublicId, workspacePublicId, cond, pageable)
								.map(ProjectResponse::from);
	}

	@Transactional(readOnly = true)
	public ProjectDetailsResponse getProjectDetails(UUID userPublicId, UUID workspacePublicId, UUID projectPublicId) {
		Project project = projectRepository.findProjectDetailsByPublicIds(userPublicId, workspacePublicId, projectPublicId)
										   .orElseThrow(() -> new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED));
		return ProjectDetailsResponse.from(project);
	}

	// 1. 무조건 프로젝트 멤버에 속한 role=ADMIN만 수정 메소드 사용 가능
	// 2. 프로젝트의 리더인 경우에만 isPrivate 값을 바꿀 수 있음
	public void updateProject(UUID userPublicId, UUID projectPublicId, UpdateProjectRequest req) {
		validateIsAdmin(userPublicId, projectPublicId);

		Project project = projectRepository.findByPublicIdWithLeader(projectPublicId)
										   .orElseThrow(() -> new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED));

		boolean isPrivateChanged = false;
		boolean prevIsPrivateVal = false;

		if (req.newIsPrivate() != null && !(project.isPrivate() == req.newIsPrivate())) {
			User leader = project.getLeader();
			validateIsLeader(userPublicId, leader);

			isPrivateChanged = true;
			prevIsPrivateVal = project.isPrivate();
		}

		project.updateProject(
				req.newName(),
				req.newDescription(),
				req.newStatus(),
				req.newStartDate(),
				req.newEndDate(),
				req.newIsPrivate()
		);

		log.info("프로젝트 정보 수정 - userId={}, projectId={}, fields={}, isPrivateChanged={}",
				userPublicId, projectPublicId, req.getUpdatedFields(),
				isPrivateChanged ? prevIsPrivateVal + " -> " + project.isPrivate() : "변경 없음");
	}

	public void deleteProject(UUID userPublicId, UUID projectPublicId) {
		Project project = projectRepository.findByPublicIdWithLeader(projectPublicId)
										   .orElseThrow(() -> new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED));
		User leader = project.getLeader();
		validateIsLeader(userPublicId, leader);

		project.softDelete();

		log.info("프로젝트 삭제 - projectId={}, leaderUserId={}", projectPublicId, userPublicId);
	}

	// 헬퍼
	private void validateIsAdmin(UUID userPublicId, UUID projectPublicId) {
		if (!projectMemberRepository.existsByProjectPublicIdAndUserPublicIdAndRole(projectPublicId, userPublicId, ProjectRole.ADMIN)) {
			throw new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}

	private void validateIsLeader(UUID userPublicId, User leader) {
		if (!leader.getPublicId().equals(userPublicId)) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}
	}
}
