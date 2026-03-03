package xyz.letzcollab.backend.dto.workspace;

import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.Workspace;
import xyz.letzcollab.backend.entity.WorkspaceMember;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceResponse(
	UUID publicId,
	String name,
	String myPosition,
	String ownerName,
	boolean isOwner,
	LocalDateTime createdAt
) {
	public static WorkspaceResponse from(WorkspaceMember member) {
		User me = member.getUser();
		Workspace workspace = member.getWorkspace();

		return new WorkspaceResponse(
				workspace.getPublicId(),
				workspace.getName(),
				member.getPosition(),
				workspace.getOwner().getName(),
				workspace.getOwner().getId().equals(me.getId()),
				workspace.getCreatedAt()
		);
	}
}
