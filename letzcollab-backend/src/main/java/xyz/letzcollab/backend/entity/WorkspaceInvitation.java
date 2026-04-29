package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
	name = "workspace_invitations",
	indexes = {
		@Index(
			name = "idx_workspace_invitations_inviter_created",
			columnList = "inviter_id, created_at"
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WorkspaceInvitation {
	@Id
	@GeneratedValue
	private Long id;

	@Column(unique = true, columnDefinition = "uuid", updatable = false, nullable = false)
	private UUID token;

	@Column(name = "invitee_email", nullable = false)
	private String inviteeEmail;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inviter_id", nullable = false)
	private User inviter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(name = "invitee_position", length = 100)
	private String inviteePosition;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "expires_at", updatable = false, nullable = false)
	private LocalDateTime expiresAt;

	private WorkspaceInvitation(User inviter, Workspace workspace, String inviteeEmail, String inviteePosition) {
		this.inviter = inviter;
		this.workspace = workspace;
		this.inviteeEmail = inviteeEmail;
		this.inviteePosition = inviteePosition;
		this.expiresAt = LocalDateTime.now().plusDays(1);
		this.token = UUID.randomUUID();
	}

	public static WorkspaceInvitation createWorkspaceInvitation(
			User inviter, Workspace workspace, String inviteeEmail, String inviteePosition
	) {
		return new WorkspaceInvitation(
				inviter, workspace, inviteeEmail,
				(inviteePosition == null || inviteePosition.isBlank()) ? null : inviteePosition
		);
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.expiresAt);
	}

	public void accept() {
		this.usedAt = LocalDateTime.now();
	}
}
