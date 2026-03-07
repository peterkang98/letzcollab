package xyz.letzcollab.backend.dto.project;

import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;

public record ProjectSearchCond(
		@Size(min = 2, message = "검색어는 2자 이상으로 입력해주세요.")
		String keyword,
		ProjectStatus status
) {
}
