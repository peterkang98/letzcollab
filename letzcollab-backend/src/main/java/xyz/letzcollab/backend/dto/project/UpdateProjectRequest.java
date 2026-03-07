package xyz.letzcollab.backend.dto.project;

import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record UpdateProjectRequest(
		@Size(min = 2, max = 100, message = "새로운 프로젝트 이름은 2자 이상 100자 이하로 입력해주세요.")
		String newName,

		@Size(max = 10000, message = "새로운 설명은 최대 10,000자까지 가능합니다.")
		String newDescription,
		ProjectStatus newStatus,
		LocalDate newStartDate,
		LocalDate newEndDate,
		Boolean newIsPrivate
) {
	public String getUpdatedFields() {
		List<String> fields = new ArrayList<>();

		if (this.newName() != null) fields.add("name");
		if (this.newDescription() != null) fields.add("description");
		if (this.newStatus() != null) fields.add("status");
		if (this.newStartDate() != null) fields.add("startDate");
		if (this.newEndDate() != null) fields.add("endDate");
		if (this.newIsPrivate() != null) fields.add("isPrivate");

		return String.join(", ", fields);
	}
}
