package xyz.letzcollab.backend.dto.workspace;

public record ProjectRawStatsDto(
	long totalProjects,
	long plannedProjects,
	long activeProjects,
	long onHoldProjects,
	long completedProjects,
	long archivedProjects
) {
}
