package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;
import xyz.letzcollab.backend.global.entity.PublicIdAndFullAuditBaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "workspaces")
public class Workspace extends PublicIdAndFullAuditBaseEntity {
	@Id
	@GeneratedValue
	@Column(name = "workspace_id")
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> members = new ArrayList<>();

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private Workspace(String name, User owner) {
		this.name = name;
		this.owner = owner;
	}

	public static Workspace createWorkspace(String name, User owner, String position) {
		Workspace workspace = new Workspace(name, owner);
		WorkspaceMember workspaceOwner = WorkspaceMember.createWorkspaceOwner(owner, workspace, position);
		workspace.members.add(workspaceOwner);
		return workspace;
	}

	public void addMember(User user, String position) {
		WorkspaceMember newMember = WorkspaceMember.createGeneralMember(user, this, position);
		this.members.add(newMember);
	}

	public void leaveWorkspace(WorkspaceMember member) {
		this.members.remove(member);
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	public void updateName(String newWorkspaceName) {
		this.name = newWorkspaceName;
	}

	public void transferOwnership(WorkspaceMember oldOwner, WorkspaceMember newOwner) {
		this.owner = newOwner.getUser();

		oldOwner.updateRoleBySystem(WorkspaceRole.ADMIN);
		newOwner.updateRoleBySystem(WorkspaceRole.OWNER);
	}
}
