package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.UpdateMyselfRequest;
import xyz.letzcollab.backend.dto.project.UpdateOtherMemberRequest;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.ProjectMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;

import xyz.letzcollab.backend.global.exception.ErrorCode;

import java.util.List;
import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectMemberService {
	private final ProjectMemberRepository projectMemberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	/**
	 * leader와 admin만 멤버 추가 가능
	 * 자기 자신의 권한보다 낮은 권한만 부여 가능 + 프로젝트 리더만 admin 권한을 부여 가능
	 * 	 - leader -> admin, member, viewer
	 * 	 - admin -> member, viewer
	 */
	public void addMember(UUID requesterPublicId, UUID workspacePublicId, UUID projectPublicId, AddMemberRequest req) {

		ProjectMember requester = projectMemberRepository.findMemberWithProjectAndLeader(projectPublicId, requesterPublicId)
														 .orElseThrow(() -> new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED));
		Project project = requester.getProject();

		validateRequesterIsAdmin(requester);

		if (req.role() == ProjectRole.ADMIN) {
			validateRequesterIsLeader(requesterPublicId, project);
		}

		WorkspaceMember workspaceMember = workspaceMemberRepository.findMemberWithUser(workspacePublicId, req.targetUserPublicId())
																   .orElseThrow(() -> new CustomException(WORKSPACE_MEMBER_NOT_FOUND));
		User user = workspaceMember.getUser();

		validateTargetIsNotProjectMember(projectPublicId, req.targetUserPublicId());

		String position = StringUtils.hasText(req.position()) ? req.position() : workspaceMember.getPosition();

		ProjectMember projectMember = switch (req.role()) {
			case ADMIN -> ProjectMember.createProjectAdmin(user, project, position);
			case MEMBER -> ProjectMember.createProjectMember(user, project, position);
			case VIEWER -> ProjectMember.createProjectViewer(user, project, position);
		};

		projectMemberRepository.save(projectMember);
		log.info("프로젝트 멤버 추가 성공 - requesterId={}, targetUserId={}, projectId={}, role={}",
				requesterPublicId, req.targetUserPublicId(), projectPublicId, req.role());
	}

	/**
	 * leader와 admin만 타 멤버 수정 가능
	 * - 자기 자신의 권한보다 낮은 권한만 부여 가능
	 * - 자기 자신의 권한보다 낮은 권한을 가진 대상만 수정 가능
	 */
	public void updateOtherMember(
			UUID requesterPublicId, UUID projectPublicId, UpdateOtherMemberRequest req
	) {
		if (requesterPublicId.equals(req.targetUserPublicId())) throw new CustomException(USE_SELF_UPDATE_API);

		List<ProjectMember> members = projectMemberRepository.findMembersWithProjectAndLeader(
				projectPublicId, requesterPublicId, req.targetUserPublicId()
		);

		ProjectMember requester = findMemberInList(members, requesterPublicId, PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		ProjectMember target = findMemberInList(members, req.targetUserPublicId(), PROJECT_MEMBER_NOT_FOUND);

		validateRequesterIsAdmin(requester);

		Project project = requester.getProject();

		// 리더가 아닌 ADMIN은 (다른 ADMIN 수정 불가 + ADMIN 권한 부여 불가)
		if (target.getRole() == ProjectRole.ADMIN || req.newRole() == ProjectRole.ADMIN) {
			validateRequesterIsLeader(requesterPublicId, project);
		}

		target.updateInfo(req.newPosition(), req.newRole());
		log.info("타인의 프로젝트 멤버 정보 수정 - requesterId={}, targetUserId={}, projectId={}, newRole={}",
				requesterPublicId, req.targetUserPublicId(), projectPublicId, req.newRole());
	}

	public void updateMyself(
			UUID userPublicId, UUID projectPublicId, UpdateMyselfRequest req
	) {
		ProjectMember me = projectMemberRepository.findByUserPublicIdAndProjectPublicId(userPublicId, projectPublicId)
												  .orElseThrow(() -> new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED));
		me.updatePosition(req.newPosition());
		log.info("본인 프로젝트 멤버 정보 수정 - userId={}, projectId={}, newPosition={}",
				userPublicId, projectPublicId, req.newPosition());
	}

	public void leaveProject(UUID userPublicId, UUID projectPublicId) {
		ProjectMember me = projectMemberRepository.findMemberWithProjectAndLeader(projectPublicId, userPublicId)
												  .orElseThrow(() -> new CustomException(PROJECT_NOT_FOUND_OR_ACCESS_DENIED));

		Project project = me.getProject();
		if (project.getLeader().getPublicId().equals(userPublicId)) {
			throw new CustomException(PROJECT_LEADER_RELEASE_REQUIRED);
		}

		projectMemberRepository.delete(me);
		log.info("프로젝트 자진 탈퇴 - userId={}, projectId={}", userPublicId, projectPublicId);
	}

	public void changeLeader(UUID requesterPublicId, UUID targetUserPublicId, UUID projectPublicId) {
		if (requesterPublicId.equals(targetUserPublicId)) throw new CustomException(CANNOT_CHANGE_LEADER_TO_SELF);

		List<ProjectMember> members = projectMemberRepository.findMembersWithProjectAndLeader(
				projectPublicId, requesterPublicId, targetUserPublicId
		);

		ProjectMember requester = findMemberInList(members, requesterPublicId, PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		ProjectMember target = findMemberInList(members, targetUserPublicId, PROJECT_MEMBER_NOT_FOUND);

		Project project = requester.getProject();
		validateRequesterIsLeader(requesterPublicId, project);

		project.changeLeader(target);
		log.info("프로젝트 리더 변경 - requesterId={}, newLeaderId={}, projectId={}",
				requesterPublicId, targetUserPublicId, projectPublicId);
	}

	/**
	 * 1. 리더는 강퇴 불가
	 * 2. 리더만 ADMIN을 강퇴 시킬 수 있음 = ADMIN은 다른 ADMIN 강퇴 불가
	 */
	public void kickMember(UUID requesterPublicId, UUID targetUserPublicId, UUID projectPublicId) {
		if (requesterPublicId.equals(targetUserPublicId)) throw new CustomException(USE_SELF_DELETE_API);

		List<ProjectMember> members = projectMemberRepository.findMembersWithProjectAndLeader(
				projectPublicId, requesterPublicId, targetUserPublicId
		);

		ProjectMember requester = findMemberInList(members, requesterPublicId, PROJECT_NOT_FOUND_OR_ACCESS_DENIED);
		ProjectMember target = findMemberInList(members, targetUserPublicId, PROJECT_MEMBER_NOT_FOUND);

		Project project = requester.getProject();

		validateTargetIsNotLeader(target, project);

		if (target.getRole() == ProjectRole.ADMIN) {
			validateRequesterIsLeader(requesterPublicId, project);
		} else {
			validateRequesterIsAdmin(requester);
		}

		projectMemberRepository.delete(target);
		log.info("프로젝트 멤버 강퇴 - requesterId={}, targetUserId={}, projectId={}",
				requesterPublicId, targetUserPublicId, projectPublicId);
	}

	// 헬퍼
	private void validateRequesterIsAdmin(ProjectMember requester) {
		if (requester.getRole() != ProjectRole.ADMIN) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}
	}

	private void validateRequesterIsLeader(UUID requesterPublicId, Project project) {
		if (!project.getLeader().getPublicId().equals(requesterPublicId)) {
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}
	}

	private void validateTargetIsNotProjectMember(UUID projectPublicId, UUID targetUserPublicId) {
		if (projectMemberRepository.existsByProjectPublicIdAndUserPublicId(projectPublicId, targetUserPublicId)) {
			throw new CustomException(ALREADY_A_PROJECT_MEMBER);
		}
	}

	private ProjectMember findMemberInList(List<ProjectMember> members, UUID userPublicId, ErrorCode errCode) {
		return members.stream()
					  .filter(m -> m.getUser().getPublicId().equals(userPublicId))
					  .findFirst()
					  .orElseThrow(() -> new CustomException(errCode));
	}

	private void validateTargetIsNotLeader(ProjectMember target, Project project) {
		User leader = project.getLeader();
		if (target.getUser().getPublicId().equals(leader.getPublicId())){
			throw new CustomException(INSUFFICIENT_PERMISSION);
		}
	}
}
