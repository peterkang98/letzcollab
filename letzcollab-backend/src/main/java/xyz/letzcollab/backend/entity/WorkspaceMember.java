package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import xyz.letzcollab.backend.entity.vo.WorkspaceRole;

import java.time.LocalDateTime;

@Getter
@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "workspace_members",
	// 특정 유저가 속한 워크스페이스 목록 조회에 유리
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_workspace_members_member_workspace",
			columnNames = {"member_id", "workspace_id"}
		)
	},
	// 특정 워크스페이스에 속한 멤버 조회 및 검증에 유리
	indexes = {
		@Index(
			name = "idx_workspace_members_workspace_member",
			columnList = "workspace_id, member_id"
		)
	}
)
public class WorkspaceMember {
	@Id
	@GeneratedValue
	@Column(name = "workspace_member_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false, updatable = false)
	private Workspace workspace;

	@Column(length = 100)
	private String position;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private WorkspaceRole role;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Builder(access = AccessLevel.PRIVATE)
	private WorkspaceMember(User user, Workspace workspace, String position, WorkspaceRole role) {
		this.user = user;
		this.workspace = workspace;
		this.position = position;
		this.role = role;
	}

	public static WorkspaceMember createWorkspaceOwner(User user, Workspace workspace, String position) {
		return WorkspaceMember.builder()
							  .user(user)
							  .workspace(workspace)
							  .position((position == null || position.isBlank()) ? null : position)
							  .role(WorkspaceRole.OWNER)
							  .build();
	}

	public static WorkspaceMember createGeneralMember(User user, Workspace workspace, String position) {
		return WorkspaceMember.builder()
							  .user(user)
							  .workspace(workspace)
							  .position((position == null || position.isBlank()) ? null : position)
							  .role(WorkspaceRole.MEMBER)
							  .build();
	}

	public boolean canInvite() {
		return this.role == WorkspaceRole.ADMIN || this.role == WorkspaceRole.OWNER;
	}

	// 본인 직책 수정용
	public void updatePosition(String newPosition) {
		this.position = (newPosition == null || newPosition.isBlank()) ? this.position : newPosition;
	}

	// 관리자 & 소유자 전용 수정 메소드
	public void updateInfo(String newPosition, WorkspaceRole newRole) {
		this.position = (newPosition == null || newPosition.isBlank()) ? this.position : newPosition;
		this.role = newRole == null ? this.role : newRole;
	}

	public boolean canUpdateOtherMember(WorkspaceMember targetMember, WorkspaceRole newRole) {
		// 최소한 관리자 권한 이상 필요
		if (!this.role.isAtLeast(WorkspaceRole.ADMIN)) {
			return false;
		}

		// 나보다 높거나 같은 권한을 가진 사람은 수정 불가
		if (!this.role.isHigherThan(targetMember.getRole())) {
			return false;
		}

		if (newRole != null) {
			// 내 권한보다 낮은 권한만 타인에게 부여 가능
			return this.role.isHigherThan(newRole);
		}
		return true;
	}

	public boolean canKickMember(WorkspaceMember targetMember) {
		// 최소한 관리자 권한 이상 필요
		if (!this.role.isAtLeast(WorkspaceRole.ADMIN)) {
			return false;
		}

		// 나보다 높거나 같은 권한을 가진 사람은 강퇴 불가
		if (!this.role.isHigherThan(targetMember.getRole())) {
			return false;
		}

		return true;
	}

	// 엔티티 계층에서만 사용가능하도록
	protected void updateRoleBySystem(WorkspaceRole role) {
		this.role = role;
	}

	public boolean canCreateProject() {
		return this.role == WorkspaceRole.ADMIN || this.role == WorkspaceRole.OWNER;
	}
}
