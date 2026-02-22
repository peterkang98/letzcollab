package xyz.letzcollab.backend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PublicIdAndFullAuditBaseEntity extends PublicIdAndDateBaseEntity {

	@CreatedBy
	@Column(name = "created_by", updatable = false, nullable = false, length = 36)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by", nullable = false, length = 36)
	private String updatedBy;
}
