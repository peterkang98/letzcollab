package xyz.letzcollab.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.letzcollab.backend.dto.task.MyTaskSearchCond;
import xyz.letzcollab.backend.dto.task.TaskSearchCond;
import xyz.letzcollab.backend.entity.Task;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepositoryCustom {

	Page<Task> findTasksByCondition(UUID projectPublicId, TaskSearchCond cond, Pageable pageable);

	Optional<Task> findTaskDetailsByPublicId(UUID taskPublicId);

	Page<Task> findMyTasks(UUID assigneePublicId, MyTaskSearchCond cond, Pageable pageable);
}
