package xyz.letzcollab.backend.dto.project;

import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

public record MyProjectMemberResponse(
		ProjectRole role,
		String position
) {
	public static MyProjectMemberResponse from(ProjectMember member) {
		return new MyProjectMemberResponse(
				member.getRole(),
				member.getPosition()
		);
	}
}
