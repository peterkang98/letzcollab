package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import xyz.letzcollab.backend.entity.vo.TokenType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "verification_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class VerificationToken {
	@Id
	@GeneratedValue
	private Long id;

	@Column(unique = true, updatable = false, nullable = false)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private TokenType type;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "expires_at", updatable = false, nullable = false)
	private LocalDateTime expiresAt;

	private VerificationToken(User user, TokenType type) {
		this.user = user;
		this.type = type;
		this.expiresAt = LocalDateTime.now().plusMinutes(30);
		this.token = UUID.randomUUID().toString();
	}

	public static VerificationToken createEmailVerificationToken(User user) {
		return new VerificationToken(user, TokenType.VERIFY_EMAIL);
	}

	public static VerificationToken createPasswordVerificationToken(User user) {
		return new VerificationToken(user, TokenType.PASSWORD_RESET);
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.expiresAt);
	}

	public void use() {
		this.usedAt = LocalDateTime.now();
	}
}
