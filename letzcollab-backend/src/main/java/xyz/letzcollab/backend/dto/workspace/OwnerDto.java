package xyz.letzcollab.backend.dto.workspace;

import xyz.letzcollab.backend.entity.User;

import java.util.UUID;

public record OwnerDto(String name, UUID publicId, String email, String phoneNumber) {
	public static OwnerDto from(User owner) {
		return new OwnerDto(owner.getName(), owner.getPublicId(), owner.getEmail(), owner.getPhoneNumber());
	}
}
