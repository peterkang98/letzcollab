package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskPriority {
	LOW("낮음", 0),
	MEDIUM("보통", 1),
	HIGH("높음", 2),
	URGENT("긴급", 3);

	private final String description;
	private final int level;
}
