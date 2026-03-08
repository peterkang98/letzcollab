package xyz.letzcollab.backend.dto.workspace;

import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.WorkspaceMember;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberSummaryDto(
	UUID publicId,
	String name,
	String email,
	String phoneNumber,
	String role,
	String position,
	LocalDateTime createdAt
) {
	public static MemberSummaryDto from(WorkspaceMember member) {
		User user = member.getUser();
		return new MemberSummaryDto(
			user.getPublicId(),
			user.getName(),
			user.getEmail(),
			user.getPhoneNumber(),
			member.getRole().getDescription(),
			member.getPosition(),
			member.getCreatedAt()
		);
	}
}
