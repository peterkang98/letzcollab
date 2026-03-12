package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "05-1. Task", description = "프로젝트 내 업무 및 하위 업무 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/projects/{projectPublicId}/tasks")
public class TaskController {

	private final TaskService taskService;

	/** 최상위 업무 생성 */
	@Operation(
			summary = "최상위 업무 생성",
			description = "프로젝트 내에 새로운 최상위 업무를 생성합니다. (프로젝트에서 VIEWER 권한을 가진 멤버는 생성 불가)"
	)
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
	@Operation(summary = "하위 업무 생성", description = "특정 상위 업무에 속하는 하위 업무(SubTask)를 생성합니다.")
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
				.fromUriString("/v1/projects/{projectPublicId}/tasks/{id}")
				.buildAndExpand(projectPublicId, subTaskPublicId)
				.toUri();

		return ResponseEntity.created(location).body(ApiResponse.success("하위 업무 생성 성공!"));
	}

	/** 업무 목록 조회 (필터 + 페이지네이션) */
	@Operation(summary = "업무 목록 조회", description = "프로젝트 내의 업무 목록을 필터 조건(상태, 담당자, 마감일 등)과 페이징에 맞게 조회합니다.")
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
	@Operation(summary = "업무 상세 조회", description = "특정 업무의 상세 정보(하위 업무 포함)를 조회합니다.")
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
	@Operation(summary = "업무 수정", description = "업무 정보를 수정합니다. ADMIN 및 REPORTER는 모든 필드 수정 가능하며, ASSIGNEE는 상태(Status)만 제한적으로 변경 가능합니다.")
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
	@Operation(summary = "업무 삭제", description = "업무를 삭제(Soft Delete)합니다. 삭제 시 하위 업무들도 연쇄 삭제됩니다. (ADMIN 또는 REPORTER만 가능)")
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
