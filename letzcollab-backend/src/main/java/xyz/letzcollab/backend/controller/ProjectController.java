package xyz.letzcollab.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import xyz.letzcollab.backend.dto.project.*;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.ProjectService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspacePublicId}/projects")
public class ProjectController {
	private final ProjectService projectService;

	/**
	 * 프로젝트 생성
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createProject(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@RequestBody @Valid CreateProjectRequest request
	) {
		UUID projectPublicId = projectService.createProject(
				userDetails.getPublicId(), workspacePublicId, request
		);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
												  .path("/{id}")
												  .buildAndExpand(projectPublicId)
												  .toUri();

		return ResponseEntity.created(location)
							 .body(ApiResponse.success("프로젝트 생성 성공!"));
	}

	/**
	 * 프로젝트 목록 조회 (검색 조건 + 페이지네이션)
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getMyProjects(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@ModelAttribute @Valid ProjectSearchCond cond,
			Pageable pageable
	) {
		Page<ProjectResponse> response = projectService.getMyProjects(
				userDetails.getPublicId(), workspacePublicId, cond, pageable
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 프로젝트 상세 조회
	 */
	@GetMapping("/{projectPublicId}")
	public ResponseEntity<ApiResponse<ProjectDetailsResponse>> getProjectDetails(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID projectPublicId
	) {
		ProjectDetailsResponse response = projectService.getProjectDetails(
				userDetails.getPublicId(), workspacePublicId, projectPublicId
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 프로젝트 정보 수정 (이름, 설명, 상태, 기간, 공개 여부)
	 */
	@PatchMapping("/{projectPublicId}")
	public ResponseEntity<ApiResponse<Void>> updateProject(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@RequestBody @Valid UpdateProjectRequest request
	) {
		projectService.updateProject(userDetails.getPublicId(), projectPublicId, request);
		return ResponseEntity.ok(ApiResponse.success("프로젝트 정보 수정 성공!"));
	}

	/**
	 * 프로젝트 삭제 (Soft Delete)
	 */
	@DeleteMapping("/{projectPublicId}")
	public ResponseEntity<ApiResponse<Void>> deleteProject(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId
	) {
		projectService.deleteProject(userDetails.getPublicId(), projectPublicId);
		return ResponseEntity.ok(ApiResponse.success("프로젝트 삭제 성공!"));
	}
}
