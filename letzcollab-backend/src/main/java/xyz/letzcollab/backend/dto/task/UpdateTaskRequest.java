package xyz.letzcollab.backend.dto.task;

import jakarta.validation.constraints.Size;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record UpdateTaskRequest(
		@Size(min = 2, max = 255, message = "업무 이름은 2자 이상 255자 이하로 입력해주세요.")
		String name,

		@Size(max = 10000, message = "설명은 최대 10,000자까지 가능합니다.")
		String description,

		TaskStatus status,

		UUID assigneePublicId,

		TaskPriority priority,

		LocalDate dueDate
) {
	public String getUpdatedFields() {
		List<String> fields = new ArrayList<>();
		if (name != null)             fields.add("name");
		if (description != null)      fields.add("description");
		if (status != null)           fields.add("status=" + status);
		if (assigneePublicId != null) fields.add("assignee");
		if (priority != null)         fields.add("priority=" + priority);
		if (dueDate != null)          fields.add("dueDate");
		return fields.toString();
	}
}
