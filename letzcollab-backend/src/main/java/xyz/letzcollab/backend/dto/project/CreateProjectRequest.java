package xyz.letzcollab.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.global.exception.CustomException;

import java.time.LocalDate;

import static xyz.letzcollab.backend.global.exception.ErrorCode.INVALID_PROJECT_DATE;

public record CreateProjectRequest(
	@NotBlank(message = "프로젝트 이름은 필수입니다.")
	@Size(min = 2, max = 100, message = "프로젝트 이름은 2자 이상 100자 이하로 입력해주세요.")
	String name,

	@Size(max = 10000, message = "설명은 최대 10,000자까지 가능합니다.")
	String description,

	@NotNull(message = "프로젝트 진행 상태를 입력해주세요")
	ProjectStatus status,

	LocalDate startDate,
	LocalDate endDate,

	@NotNull(message = "공개 여부는 필수입니다.")
	Boolean isPrivate,

	@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요. (예: 백엔드 개발, UI 디자인)")
	String position
) {
	public CreateProjectRequest {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new CustomException(INVALID_PROJECT_DATE);
		}
	}
}
