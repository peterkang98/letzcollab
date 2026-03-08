package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import xyz.letzcollab.backend.entity.vo.ProjectRole;

import java.time.LocalDateTime;

@Getter
@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
		name = "project_members",
		uniqueConstraints = {
			@UniqueConstraint(
					name = "uk_project_members_project_member",
					columnNames = {"project_id", "member_id"}
			)
		}
)
public class ProjectMember {
	@Id
	@GeneratedValue
	@Column(name = "project_member_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false, updatable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectRole role;

	// 멤버별로 해당 프로젝트에서 담당하는 역할 (예: 인프라 구축 담당, UI 디자인 담당)
	@Column(nullable = false, length = 100)
	private String position;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Builder(access = AccessLevel.PRIVATE)
	private ProjectMember(Project project, User user, ProjectRole role, String position) {
		this.project = project;
		this.user = user;
		this.role = role;
		this.position = position;
	}

	public static ProjectMember createProjectAdmin(User leader, Project project, String position) {
		return ProjectMember.builder()
							.user(leader)
							.project(project)
							.role(ProjectRole.ADMIN)
							.position((position == null || position.isBlank()) ? "프로젝트 리더" : position)
							.build();
	}

	public static ProjectMember createProjectMember(User member, Project project, String position) {
		return ProjectMember.builder()
							.user(member)
							.project(project)
							.role(ProjectRole.MEMBER)
							.position((position == null || position.isBlank()) ? "프로젝트 참여자" : position)
							.build();
	}

	public static ProjectMember createProjectViewer(User viewer, Project project, String position) {
		return ProjectMember.builder()
							.user(viewer)
							.project(project)
							.role(ProjectRole.VIEWER)
							.position((position == null || position.isBlank()) ? "프로젝트 조회자" : position)
							.build();
	}

	public void updatePosition(String newPosition) {
		if (newPosition != null && !newPosition.isBlank()) {
			this.position = newPosition;
		}
	}

	public void updateInfo(String newPosition, ProjectRole newRole) {
		if (newPosition != null && !newPosition.isBlank()) {
			this.position = newPosition;
		}
		if (newRole != null) {
			this.role = newRole;
		}
	}

	// 엔티티 계층에서만 사용 (changeLeader 전용)
	protected void updateRoleBySystem(ProjectRole role) {
		this.role = role;
	}
}
