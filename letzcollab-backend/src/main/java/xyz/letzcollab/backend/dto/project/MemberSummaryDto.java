package xyz.letzcollab.backend.dto.project;

import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberSummaryDto(
		UUID publicId,
		String name,
		String email,
		String phoneNumber,
		ProjectRole role,
		String position,
		LocalDateTime createdAt

) {
	public static MemberSummaryDto from(ProjectMember projectMember) {
		User user = projectMember.getUser();
		return new MemberSummaryDto(
				user.getPublicId(),
				user.getName(),
				user.getEmail(),
				user.getPhoneNumber(),
				projectMember.getRole(),
				projectMember.getPosition(),
				projectMember.getCreatedAt()
		);
	}
}
