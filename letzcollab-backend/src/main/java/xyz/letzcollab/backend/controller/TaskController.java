package xyz.letzcollab.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import xyz.letzcollab.backend.dto.task.*;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.TaskService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectPublicId}/tasks")
public class TaskController {

	private final TaskService taskService;

	/** 최상위 업무 생성 */
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createTask(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@RequestBody @Valid CreateTaskRequest request
	) {
		UUID taskPublicId = taskService.createTask(userDetails.getPublicId(), projectPublicId, request);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
												  .path("/{id}")
												  .buildAndExpand(taskPublicId)
												  .toUri();

		return ResponseEntity.created(location).body(ApiResponse.success("업무 생성 성공!"));
	}

	/** 하위 업무 생성 */
	@PostMapping("/{parentTaskPublicId}/subtasks")
	public ResponseEntity<ApiResponse<Void>> createSubTask(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID parentTaskPublicId,
			@RequestBody @Valid CreateTaskRequest request
	) {
		UUID subTaskPublicId = taskService.createSubTask(
				userDetails.getPublicId(), projectPublicId, parentTaskPublicId, request);

		URI location = ServletUriComponentsBuilder
				.fromUriString("/api/v1/projects/{projectPublicId}/tasks/{id}")
				.buildAndExpand(projectPublicId, subTaskPublicId)
				.toUri();

		return ResponseEntity.created(location).body(ApiResponse.success("하위 업무 생성 성공!"));
	}

	/** 업무 목록 조회 (필터 + 페이지네이션) */
	@GetMapping
	public ResponseEntity<ApiResponse<Page<TaskResponse>>> getTasks(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@ModelAttribute @Valid TaskSearchCond cond,
			Pageable pageable
	) {
		Page<TaskResponse> response = taskService.getTasks(userDetails.getPublicId(), projectPublicId, cond, pageable);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/** 업무 상세 조회 */
	@GetMapping("/{taskPublicId}")
	public ResponseEntity<ApiResponse<TaskDetailsResponse>> getTaskDetails(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID taskPublicId
	) {
		TaskDetailsResponse response = taskService.getTaskDetails(userDetails.getPublicId(), projectPublicId, taskPublicId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/** 업무 수정 */
	@PatchMapping("/{taskPublicId}")
	public ResponseEntity<ApiResponse<Void>> updateTask(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID taskPublicId,
			@RequestBody @Valid UpdateTaskRequest request
	) {
		taskService.updateTask(userDetails.getPublicId(), projectPublicId, taskPublicId, request);
		return ResponseEntity.ok(ApiResponse.success("업무 수정 성공!"));
	}

	/** 업무 삭제 (Soft Delete, 하위 업무 연쇄 삭제) */
	@DeleteMapping("/{taskPublicId}")
	public ResponseEntity<ApiResponse<Void>> deleteTask(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID taskPublicId
	) {
		taskService.deleteTask(userDetails.getPublicId(), projectPublicId, taskPublicId);
		return ResponseEntity.ok(ApiResponse.success("업무 삭제 성공!"));
	}
}
