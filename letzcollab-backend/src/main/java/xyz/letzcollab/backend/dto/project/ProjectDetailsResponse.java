package xyz.letzcollab.backend.dto.project;

import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.ProjectMember;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectDetailsResponse(
	UUID projectPublicId,
	String projectName,
	String description,
	ProjectStatus status,
	LocalDate startDate,
	LocalDate endDate,
	boolean isPrivate,
	LeaderDto leader,
	List<MemberSummaryDto> members,
	int memberCount,
	LocalDateTime createdAt
) {
	public static ProjectDetailsResponse from(Project project) {
		User projectLeader = project.getLeader();
		List<ProjectMember> projectMembers = project.getMembers();

		return new ProjectDetailsResponse(
				project.getPublicId(),
				project.getName(),
				project.getDescription(),
				project.getStatus(),
				project.getStartDate(),
				project.getEndDate(),
				project.isPrivate(),
				LeaderDto.from(projectLeader),
				projectMembers.stream().map(MemberSummaryDto::from).toList(),
				projectMembers.size(),
				project.getCreatedAt()
		);
	}

}
