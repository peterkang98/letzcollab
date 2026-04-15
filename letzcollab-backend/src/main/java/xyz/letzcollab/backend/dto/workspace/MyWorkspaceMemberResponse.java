package xyz.letzcollab.backend.dto.workspace;

import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;

public record MyWorkspaceMemberResponse(
		WorkspaceRole role,
		String position
) {
	public static MyWorkspaceMemberResponse from(WorkspaceMember me) {
		return new MyWorkspaceMemberResponse(me.getRole(), me.getPosition());
	}
}
