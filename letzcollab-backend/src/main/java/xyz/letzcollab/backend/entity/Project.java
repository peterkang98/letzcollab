package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import xyz.letzcollab.backend.entity.vo.ProjectRole;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.global.entity.PublicIdAndFullAuditBaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "projects",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_projects_workspace_name",
			columnNames = {"workspace_id", "name"}
		)
	}
)
public class Project extends PublicIdAndFullAuditBaseEntity {
	@Id
	@GeneratedValue
	@Column(name = "project_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(length = 100, nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private ProjectStatus status;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "is_private", nullable = false)
	private boolean isPrivate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private User leader;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProjectMember> members = new ArrayList<>();

	@Builder(access = AccessLevel.PRIVATE)
	private Project(
			Workspace workspace, String name, String description, ProjectStatus status, LocalDate startDate,
			LocalDate endDate, boolean isPrivate, User leader
	) {
		this.workspace = workspace;
		this.name = name;
		this.description = description;
		this.status = status;
		this.startDate = startDate;
		this.endDate = endDate;
		this.isPrivate = isPrivate;
		this.leader = leader;
	}

	public static Project createProject(
			Workspace workspace, String name, String description, ProjectStatus status, LocalDate startDate,
			LocalDate endDate, boolean isPrivate, User leader, String position
	) {
		Project project = Project.builder()
								 .workspace(workspace)
								 .name(name)
								 .description(description)
								 .status(status)
								 .startDate(startDate)
								 .endDate(endDate)
								 .isPrivate(isPrivate)
								 .leader(leader)
								 .build();

		ProjectMember leaderMember = ProjectMember.createProjectAdmin(leader, project, position);
		project.getMembers().add(leaderMember);

		return project;
	}

	public void updateProject(String name, String description, ProjectStatus status, LocalDate startDate,
							  LocalDate endDate, Boolean isPrivate) {
		if (name != null) this.name = name;
		if (description != null) this.description = description;
		if (status != null) this.status = status;
		if (startDate != null) this.startDate = startDate;
		if (endDate != null) this.endDate = endDate;

		if (isPrivate != null) {
			this.isPrivate = isPrivate;
		}
	}

	public void changeLeader(ProjectMember newLeader) {
		newLeader.updateRoleBySystem(ProjectRole.ADMIN);
		this.leader = newLeader.getUser();
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
		this.name = "deleted" + this.name + "_" + System.currentTimeMillis();
	}
}
