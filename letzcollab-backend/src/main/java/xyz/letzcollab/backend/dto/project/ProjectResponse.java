package xyz.letzcollab.backend.dto.project;

import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
	UUID publicId,
	String name,
	ProjectStatus status,
	LocalDate startDate,
	LocalDate endDate,
	boolean isPrivate,
	String leaderName,
	long memberCount,
	LocalDateTime createdAt
) {
	public static ProjectResponse from(Project p) {
		return new ProjectResponse(
			p.getPublicId(),
			p.getName(),
			p.getStatus(),
			p.getStartDate(),
			p.getEndDate(),
			p.isPrivate(),
			p.getLeader().getName(),
			p.getMembers().size(),
			p.getCreatedAt()
		);
	}
}
