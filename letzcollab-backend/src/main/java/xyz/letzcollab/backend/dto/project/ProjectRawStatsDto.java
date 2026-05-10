package xyz.letzcollab.backend.dto.project;

public record ProjectRawStatsDto(
	long totalProjects,
	long plannedProjects,
	long activeProjects,
	long onHoldProjects,
	long completedProjects,
	long archivedProjects
) {
}
