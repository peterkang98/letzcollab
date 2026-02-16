package xyz.letzcollab.backend.entity.vo;

import io.jsonwebtoken.JwtException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum UserRole {
	USER("ROLE_USER"),
	ADMIN("ROLE_ADMIN");

	private final String authority;

	private static final Map<String, UserRole> ROLE_MAP = Arrays.stream(values())
																.collect(Collectors.toMap(UserRole::getAuthority, r -> r));

	public static UserRole fromAuthority(String authority) {
		UserRole userRole = ROLE_MAP.get(authority);

		if (userRole == null) {
			throw new JwtException("유효하지 않은 권한이 포함된 토큰입니다.");
		}
		return userRole;
	}
}
