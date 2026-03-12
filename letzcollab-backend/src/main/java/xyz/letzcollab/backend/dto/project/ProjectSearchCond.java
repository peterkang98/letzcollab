package xyz.letzcollab.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;

@Schema(description = "프로젝트 검색 조건 DTO")
public record ProjectSearchCond(
		@Schema(description = "검색어 (프로젝트 이름 포함)", example = "데이터베이스", nullable = true)
		@Size(min = 2, message = "검색어는 2자 이상으로 입력해주세요.")
		String keyword,
		@Schema(description = "프로젝트 진행 상태 필터", example = "ACTIVE")
		ProjectStatus status
) {
}
