package xyz.letzcollab.backend.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;
import xyz.letzcollab.backend.global.exception.CustomException;

import java.time.LocalDate;

import static xyz.letzcollab.backend.global.exception.ErrorCode.INVALID_PROJECT_DATE;

@Schema(description = "프로젝트 생성 요청 DTO")
public record CreateProjectRequest(
	@Schema(description = "프로젝트 이름", example = "서비스 리팩토링")
	@NotBlank(message = "프로젝트 이름은 필수입니다.")
	@Size(min = 2, max = 100, message = "프로젝트 이름은 2자 이상 100자 이하로 입력해주세요.")
	String name,

	@Schema(description = "프로젝트 설명", example = "레거시 코드를 개선하는 프로젝트입니다.", nullable = true)
	@Size(max = 10000, message = "설명은 최대 10,000자까지 가능합니다.")
	String description,

	@Schema(description = "프로젝트 진행 상태: PLANNED/ACTIVE/ON_HOLD/COMPLETED/ARCHIVED", example = "PLANNED")
	@NotNull(message = "프로젝트 진행 상태를 입력해주세요")
	ProjectStatus status,

	@Schema(description = "시작일", example = "2024-03-01", nullable = true)
	LocalDate startDate,

	@Schema(description = "종료일", example = "2024-12-31", nullable = true)
	LocalDate endDate,

	@Schema(description = "비공개 여부 (true: 멤버만 확인 가능, false: 워크스페이스 전체 공개)", example = "false", nullable = true)
	@NotNull(message = "공개 여부는 필수입니다.")
	Boolean isPrivate,

	@Schema(description = "본인의 프로젝트에서 본인 역할", example = "백엔드 개발", nullable = true)
	@Size(min = 2, max = 100, message = "역할은 2자 이상 100자 이하로 상세히 입력해주세요. (예: 백엔드 개발, UI 디자인)")
	String position
) {
	public CreateProjectRequest {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new CustomException(INVALID_PROJECT_DATE);
		}
	}
}
