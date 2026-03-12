package xyz.letzcollab.backend.dto.user;

import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.UserStatus;

import java.util.UUID;

public record UserResponse(
		UUID publicId,
		String email,
		String name,
		String phoneNumber,
		UserStatus status
) {
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getPublicId(),
				user.getEmail(),
				user.getName(),
				user.getPhoneNumber(),
				user.getStatus()
		);
	}
}
