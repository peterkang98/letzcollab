package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.workspace.WorkspaceDetailsResponse;
import xyz.letzcollab.backend.dto.workspace.WorkspaceResponse;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.repository.WorkspaceMemberRepository;
import xyz.letzcollab.backend.repository.WorkspaceRepository;

import java.util.List;
import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.*;


/**
 * WorkspaceService(공간 중심)
 * - 워크스페이스 생성/수정/삭제
 * - 워크스페이스 상세 정보 및 전체 멤버 목록 조회
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;

	public UUID createWorkspace(UUID userPublicId, String workspaceName, String position) {
		User owner = userRepository.findByPublicId(userPublicId)
								   .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		if (workspaceRepository.existsByName(workspaceName)) {
			throw new CustomException(DUPLICATE_WORKSPACE_NAME);
		}

		Workspace workspace = Workspace.createWorkspace(workspaceName, owner, position);
		workspaceRepository.save(workspace);
		log.info("워크스페이스 생성 - workspaceName={}, ownerUserId={}", workspaceName, userPublicId);

		return workspace.getPublicId();
	}

	@Transactional(readOnly = true)
	public List<WorkspaceResponse> getMyWorkspaces(UUID userPublicId) {
		List<WorkspaceMember> workspaceMembers = workspaceMemberRepository.findAllWithWorkspace(userPublicId);

		return workspaceMembers.stream()
							   .map(WorkspaceResponse::from)
							   .toList();
	}

	@Transactional(readOnly = true)
	public WorkspaceDetailsResponse getWorkspaceDetails(UUID userPublicId, UUID workspacePublicId) {
		validateMemberAndWorkspaceExistence(userPublicId, workspacePublicId);

		// 전체 멤버 조회
		Workspace foundWorkspace = workspaceRepository.findWorkspaceWithAllMembers(workspacePublicId)
													  .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));

		WorkspaceMember me = foundWorkspace.getMembers()
										   .stream()
										   .filter(member -> member.getUser().getPublicId().equals(userPublicId))
										   .findFirst()
										   .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));

		return WorkspaceDetailsResponse.from(foundWorkspace, me);
	}

	public void updateWorkspace(UUID userPublicId, UUID workspacePublicId, String newWorkspaceName) {
		Workspace foundWorkspace = getWorkspaceAndCheckOwner(userPublicId, workspacePublicId);
		foundWorkspace.updateName(newWorkspaceName);
		log.info("워크스페이스 이름 수정 - workspaceId={}, ownerUserId={}, newWorkspaceName={}",
				workspacePublicId, userPublicId, newWorkspaceName);
	}


	public void deleteWorkspace(UUID userPublicId, UUID workspacePublicId){
		Workspace foundWorkspace = getWorkspaceAndCheckOwner(userPublicId, workspacePublicId);
		foundWorkspace.softDelete();
		log.info("워크스페이스 삭제 - workspaceId={}, ownerUserId={}", workspacePublicId, userPublicId);
	}


	// 헬퍼 메소드
	private void validateMemberAndWorkspaceExistence(UUID userPublicId, UUID workspacePublicId) {
		if(!workspaceMemberRepository.existsByWorkspacePublicIdAndUserPublicId(workspacePublicId, userPublicId)) {
			throw new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}
	}

	private Workspace getWorkspaceAndCheckOwner(UUID userPublicId, UUID workspacePublicId) {
		Workspace foundWorkspace = workspaceRepository.findWorkspaceByPublicIdWithOwner(workspacePublicId)
													  .orElseThrow(() -> new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED));
		User owner = foundWorkspace.getOwner();
		if (!owner.getPublicId().equals(userPublicId)) {
			throw new CustomException(WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED);
		}
		return foundWorkspace;
	}
}