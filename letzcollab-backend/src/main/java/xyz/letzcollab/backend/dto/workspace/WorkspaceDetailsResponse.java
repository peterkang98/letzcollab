package xyz.letzcollab.backend.dto.workspace;

import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WorkspaceDetailsResponse(
	UUID workspacePublicId,
	String workspaceName,
	OwnerDto owner,
	String myPosition,
	boolean isOwner,
	List<MemberSummaryDto> members,
	int memberCount,
	LocalDateTime createdAt
) {
	public static WorkspaceDetailsResponse from(Workspace workspace, WorkspaceMember me) {
		return new WorkspaceDetailsResponse(
				workspace.getPublicId(),
				workspace.getName(),
				OwnerDto.from(workspace.getOwner()),
				me.getPosition(),
				me.getUser().getId().equals(workspace.getOwner().getId()),
				workspace.getMembers().stream().map(MemberSummaryDto::from).toList(),
				workspace.getMembers().size(),
				workspace.getCreatedAt()
		);
	}
}
