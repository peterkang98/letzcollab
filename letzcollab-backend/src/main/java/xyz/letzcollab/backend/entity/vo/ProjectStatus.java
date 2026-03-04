package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus {
	PLANNED("기획됨"),
	ACTIVE("진행 중"),
	ON_HOLD("일시 중단"),
	COMPLETED("완료"),
	ARCHIVED("보관됨");

	private final String description;
}
