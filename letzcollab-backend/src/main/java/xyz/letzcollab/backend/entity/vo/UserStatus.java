package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {

	ACTIVE("정상", true, true),
	PENDING("이메일 미인증", false, true),
	BANNED("정지 계정", true, false),
	DELETED("탈퇴 계정", false, false);

	private final String description;
	private final boolean enabled;
	private final boolean notLocked;

	public boolean canLogin() {
		return (this.enabled && this.notLocked);
	}

	public boolean canResetPassword() {
		return this.notLocked;
	}
}
