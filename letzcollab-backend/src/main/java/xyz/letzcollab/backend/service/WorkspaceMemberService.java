package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceInvitation;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;
import xyz.letzcollab.backend.global.email.EmailService;
import xyz.letzcollab.backend.global.email.context.WorkspaceInvitationEmailContext;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.global.exception.ErrorCode;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceInvitationRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;

import java.util.List;
import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;

/**
 * WorkspaceMemberService (사람/권한 중심)
 * - 초대 및 수락
 * - 타인의 정보/권한 수정 및 본인 정보 수정
 * - 멤버 강퇴 및 자진 탈퇴
 * - 소유권 이전
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WorkspaceMemberService {

	private final WorkspaceMemberRepository memberRepository;
	private final WorkspaceInvitationRepository invitationRepository;
	private final EmailService emailService;
	private final UserRepository userRepository;

	@Value("${frontend.base-url}")
	private String frontendURL;

	public void inviteMemberByEmail(UUID userPublicId, UUID workspacePublicId, String inviteeEmail, String inviteePosition) {
		WorkspaceMember requester = memberRepository.findMemberWithWorkspaceAndUser(workspacePublicId, userPublicId)
													.orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));

		validateInviterPermissionAndInviteeExistence(workspacePublicId, inviteeEmail, requester);

		User inviter = requester.getUser();
		Workspace workspace = requester.getWorkspace();

		WorkspaceInvitation invitation = WorkspaceInvitation.createWorkspaceInvitation(inviter, workspace, inviteeEmail, inviteePosition);
		invitationRepository.save(invitation);

		WorkspaceInvitationEmailContext context = new WorkspaceInvitationEmailContext(
				inviter.getName(),
				workspace.getName(),
				invitation.getToken().toString(),
				frontendURL
		);
		emailService.sendTemplateEmail(inviteeEmail, context);
		log.info("워크스페이스 초대 이메일 전송 - workspaceId={}, inviterUserId={}, inviteeEmail={}",
				workspacePublicId, userPublicId, inviteeEmail);
	}

	public void acceptInvitation(UUID userPublicId, UUID token) {
		WorkspaceInvitation invitation = invitationRepository.findByTokenWithWorkspace(token)
															 .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));
		validateInvitation(invitation);

		User user = userRepository.findByPublicId(userPublicId)
								  .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		if (!invitation.getInviteeEmail().equals(user.getEmail())) {
			throw new CustomException(WORKSPACE_INVITEE_MISMATCH);
		}

		Workspace workspace = invitation.getWorkspace();

		if (memberRepository.existsByWorkspaceAndUser(workspace, user)) {
			throw new CustomException(ALREADY_A_WORKSPACE_MEMBER);
		}

		workspace.addMember(user, invitation.getInviteePosition());
		invitation.accept();
		log.info("워크스페이스 초대 수락 - workspaceId={}, userId={}", workspace.getPublicId(), userPublicId);
	}

	public void updateMyself(UUID workspacePublicId, UUID userPublicId, String newPosition) {
		WorkspaceMember me = memberRepository.findByUserAndWorkspacePublicId(workspacePublicId, userPublicId)
											 .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));

		me.updatePosition(newPosition);
		log.info("본인 직책 수정 - workspaceId={}, userId={}, newPosition={}", workspacePublicId, userPublicId, newPosition);
	}

	// 타인을 수정
	public void updateOtherMember(
			UUID requesterUserPublicId, UUID workspacePublicId, UUID targetMemberUserPublicId,
			String newPosition, WorkspaceRole newRole
	) {
		if (requesterUserPublicId.equals(targetMemberUserPublicId)) throw new CustomException(USE_SELF_UPDATE_API);

		List<WorkspaceMember> members = memberRepository.findMembersForUpdate(
				workspacePublicId, requesterUserPublicId, targetMemberUserPublicId
		);

		// 요청자를 찾을 수 없었다면, 보안상 범용적인 워크스페이스 오류코드를 반환
		WorkspaceMember requester = findMemberInList(members, requesterUserPublicId, WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		WorkspaceMember targetMember = findMemberInList(members, targetMemberUserPublicId, WORKSPACE_MEMBER_NOT_FOUND);

		if (!requester.canUpdateOtherMember(targetMember, newRole)) {
			throw new CustomException(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		targetMember.updateInfo(newPosition, newRole);
		log.info("타인의 멤버 정보 수정 - workspaceId={}, requesterId={}, targetUserId={}, newRole={}",
				workspacePublicId, requesterUserPublicId, targetMemberUserPublicId, newRole);
	}


	// 자진 탈퇴
	public void leaveWorkspace(UUID userPublicId, UUID workspacePublicId) {
		WorkspaceMember me = memberRepository.findMemberWithWorkspaceAndUserAndOwner(workspacePublicId, userPublicId)
											 .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));

		Workspace workspace = me.getWorkspace();
		User owner = workspace.getOwner();
		User withdrawer = me.getUser();

		if (withdrawer.getId().equals(owner.getId())) {
			throw new CustomException(WORKSPACE_OWNER_RELEASE_REQUIRED);
		}
		workspace.leaveWorkspace(me);
		log.info("워크스페이스 자진 탈퇴 - workspaceId={}, userId={}", workspacePublicId, userPublicId);
	}

	public void kickMember(UUID requesterUserPublicId, UUID workspacePublicId, UUID targetMemberUserPublicId) {
		if (requesterUserPublicId.equals(targetMemberUserPublicId)) throw new CustomException(USE_SELF_DELETE_API);

		List<WorkspaceMember> members = memberRepository.findMembersWithWorkspace(
				workspacePublicId, requesterUserPublicId, targetMemberUserPublicId
		);

		WorkspaceMember requester = findMemberInList(members, requesterUserPublicId, WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		WorkspaceMember targetMember = findMemberInList(members, targetMemberUserPublicId, WORKSPACE_MEMBER_NOT_FOUND);

		if (!requester.canKickMember(targetMember)) {
			throw new CustomException(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		Workspace workspace = requester.getWorkspace();
		workspace.leaveWorkspace(targetMember);
		log.info("멤버 강퇴 - workspaceId={}, requesterId={}, targetUserId={}",
				workspacePublicId, requesterUserPublicId, targetMemberUserPublicId);
	}

	public void transferOwnership(UUID requesterUserPublicId, UUID workspacePublicId, UUID targetMemberUserPublicId) {
		if (requesterUserPublicId.equals(targetMemberUserPublicId)) throw new CustomException(CANNOT_TRANSFER_OWNERSHIP_TO_SELF);

		List<WorkspaceMember> members = memberRepository.findMembersWithWorkspace(
				workspacePublicId, requesterUserPublicId, targetMemberUserPublicId
		);

		WorkspaceMember requester = findMemberInList(members, requesterUserPublicId, WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		WorkspaceMember targetMember = findMemberInList(members, targetMemberUserPublicId, WORKSPACE_MEMBER_NOT_FOUND);


		Workspace workspace = requester.getWorkspace();
		workspace.transferOwnership(requester, targetMember);
		log.info("소유권 이전 - workspaceId={}, requesterId={}, targetUserId={}",
				workspacePublicId, requesterUserPublicId, targetMemberUserPublicId);
	}

	// 헬퍼 메소드
	private void validateInviterPermissionAndInviteeExistence(UUID workspacePublicId, String inviteeEmail, WorkspaceMember requester) {
		if (!requester.canInvite()) {
			throw new CustomException(INSUFFICIENT_WORKSPACE_PERMISSION);
		}

		if (memberRepository.existsByWorkspacePublicIdAndUserEmail(workspacePublicId, inviteeEmail)) {
			throw new CustomException(ALREADY_A_WORKSPACE_MEMBER);
		}
	}

	private void validateInvitation(WorkspaceInvitation invitation) {
		if (invitation.isExpired() || invitation.getUsedAt() != null) {
			throw new CustomException(WORKSPACE_INVITATION_INVALID);
		}
	}

	private static WorkspaceMember findMemberInList(List<WorkspaceMember> members, UUID userPublicId, ErrorCode errCode) {
		return members.stream()
					  .filter(m -> m.getUser().getPublicId().equals(userPublicId))
					  .findFirst()
					  .orElseThrow(() -> new CustomException(errCode));
	}
}
