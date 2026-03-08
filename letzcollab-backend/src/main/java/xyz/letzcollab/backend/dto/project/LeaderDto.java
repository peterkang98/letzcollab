package xyz.letzcollab.backend.dto.project;

import xyz.letzcollab.backend.entity.User;

import java.util.UUID;

public record LeaderDto(
	String name, UUID publicId, String email, String phoneNumber
) {
	public static LeaderDto from(User projectLeader) {
		return new LeaderDto(
				projectLeader.getName(),
				projectLeader.getPublicId(),
				projectLeader.getEmail(),
				projectLeader.getPhoneNumber()
		);
	}
}
