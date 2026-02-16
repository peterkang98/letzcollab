package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.entity.vo.UserStatus;
import xyz.letzcollab.backend.global.entity.PublicIdAndDateBaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends PublicIdAndDateBaseEntity {
	@Id
	@GeneratedValue
	@Column(name = "user_id")
	private Long id;

	@Column(length = 100, nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(name = "phone_number", length = 13)
	private String phoneNumber;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private UserStatus status;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole role;

	@Builder(access = AccessLevel.PRIVATE)
	private User(String name, String email, String password, String phoneNumber, UserStatus status, UserRole role) {
		this.name = name;
		this.email = email;
		this.password = password;
		this.phoneNumber = phoneNumber;
		this.status = status;
		this.role = role;
	}

	// 일반 회원가입용
	public static User createPendingUser(String name, String email, String password, String phoneNumber) {
		return User.builder()
				   .name(name)
				   .email(email)
				   .password(password)
				   .phoneNumber((phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber)
				   .status(UserStatus.PENDING)
				   .role(UserRole.USER)
				   .build();
	}

	// 관리자 계정 생성용
	public static User createAdminUser(String name, String email, String password, String phoneNumber) {
		return User.builder()
				   .name(name)
				   .email(email)
				   .password(password)
				   .phoneNumber(phoneNumber)
				   .status(UserStatus.ACTIVE)
				   .role(UserRole.ADMIN)
				   .build();
	}

	// 더미 데이터 생성용
	public static User createDummyUser(String name, String email, String password, String phoneNumber) {
		return User.builder()
				   .name(name)
				   .email(email)
				   .password(password)
				   .phoneNumber(phoneNumber)
				   .status(UserStatus.ACTIVE)
				   .role(UserRole.USER)
				   .build();
	}

	public void verifyEmail() {
		if (this.status == UserStatus.PENDING){
			this.status = UserStatus.ACTIVE;
		}
	}

	public void resetPassword(String newPassword) {
		this.password = newPassword;
	}
}
