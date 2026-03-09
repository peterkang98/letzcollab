package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
	TODO("할 일"),
	IN_PROGRESS("진행 중"),
	IN_REVIEW("검토 중"),
	DONE("완료"),
	CANCELLED("취소됨");

	private final String description;
}
