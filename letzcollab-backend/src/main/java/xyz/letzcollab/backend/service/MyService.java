package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.task.MyTaskResponse;
import xyz.letzcollab.backend.dto.task.MyTaskSearchCond;
import xyz.letzcollab.backend.repository.TaskRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyService {

	private final TaskRepository taskRepository;

	@Transactional(readOnly = true)
	public Page<MyTaskResponse> getMyTasks(UUID userPublicId, MyTaskSearchCond cond, Pageable pageable) {
		return taskRepository.findMyTasks(userPublicId, cond, pageable)
							 .map(MyTaskResponse::from);
	}
}