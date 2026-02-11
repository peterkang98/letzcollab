package xyz.letzcollab.backend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PublicIdAndDateBaseEntity extends DateBaseEntity {

	@Column(name = "public_id", length = 36, updatable = false, nullable = false, unique = true)
	private String publicId;

	@PrePersist
	private void generatePublicId() {
		this.publicId = UUID.randomUUID().toString();
	}
}
