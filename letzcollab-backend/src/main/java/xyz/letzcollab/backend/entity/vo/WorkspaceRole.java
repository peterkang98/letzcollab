package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceRole {
	OWNER("소유자", 3),
	ADMIN("관리자", 2),
	MEMBER("일반 멤버", 1),
	GUEST("외부 협력자", 0);

	private final String description;
	private final int level;

	public boolean isHigherThan(WorkspaceRole other) {
		return this.level > other.level;
	}

	public boolean isAtLeast(WorkspaceRole minRole) {
		return this.level >= minRole.level;
	}
}
