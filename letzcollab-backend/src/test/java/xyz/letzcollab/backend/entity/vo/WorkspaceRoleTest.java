package xyz.letzcollab.backend.entity.vo;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkspaceRole 권한 레벨 비교")
class WorkspaceRoleTest {

	@ParameterizedTest(name = "{0}.isHigherThan({1}) = {2}")
	@CsvSource({
			"OWNER, ADMIN,  true",
			"OWNER, MEMBER, true",
			"OWNER, GUEST,  true",
			"ADMIN, MEMBER, true",
			"ADMIN, GUEST,  true",
			"MEMBER, GUEST, true",
			"ADMIN, OWNER,  false",
			"MEMBER, ADMIN, false",
			"GUEST, MEMBER, false",
			"OWNER, OWNER,  false",  // 동급은 higher 아님
			"ADMIN, ADMIN,  false",
	})
	@DisplayName("isHigherThan 검증")
	void isHigherThan(WorkspaceRole subject, WorkspaceRole other, boolean expected) {
		assertThat(subject.isHigherThan(other)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{0}.isAtLeast({1}) = {2}")
	@CsvSource({
			"OWNER, OWNER,  true",
			"OWNER, ADMIN,  true",
			"OWNER, MEMBER, true",
			"OWNER, GUEST,  true",
			"ADMIN, ADMIN,  true",
			"ADMIN, MEMBER, true",
			"ADMIN, GUEST,  true",
			"MEMBER, MEMBER, true",
			"MEMBER, GUEST,  true",
			"GUEST,  GUEST,  true",
			"ADMIN,  OWNER,  false",
			"MEMBER, ADMIN,  false",
			"GUEST,  MEMBER, false",
	})
	@DisplayName("isAtLeast 검증")
	void isAtLeast(WorkspaceRole subject, WorkspaceRole minRole, boolean expected) {
		assertThat(subject.isAtLeast(minRole)).isEqualTo(expected);
	}

	@Test
	@DisplayName("권한 레벨 순서: OWNER > ADMIN > MEMBER > GUEST")
	void levelOrder() {
		assertThat(WorkspaceRole.OWNER.getLevel())
				.isGreaterThan(WorkspaceRole.ADMIN.getLevel())
				.isGreaterThan(WorkspaceRole.MEMBER.getLevel())
				.isGreaterThan(WorkspaceRole.GUEST.getLevel());
	}
}