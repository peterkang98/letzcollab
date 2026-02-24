package xyz.letzcollab.backend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.util.UUID;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PublicIdAndFullAuditBaseEntity extends PublicIdAndDateBaseEntity {

	@CreatedBy
	@Column(name = "created_by", columnDefinition = "uuid",  updatable = false, nullable = false, length = 36)
	private UUID createdBy;

	@LastModifiedBy
	@Column(name = "updated_by", columnDefinition = "uuid", nullable = false, length = 36)
	private UUID updatedBy;
}
