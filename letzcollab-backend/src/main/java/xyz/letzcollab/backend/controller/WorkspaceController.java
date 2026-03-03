package xyz.letzcollab.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import xyz.letzcollab.backend.dto.workspace.CreateWorkspaceRequest;
import xyz.letzcollab.backend.dto.workspace.UpdateWorkspaceRequest;
import xyz.letzcollab.backend.dto.workspace.WorkspaceDetailsResponse;
import xyz.letzcollab.backend.dto.workspace.WorkspaceResponse;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.WorkspaceService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {
	private final WorkspaceService workspaceService;

	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createWorkspace(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody CreateWorkspaceRequest request
	) {
		UUID workspacePublicId = workspaceService.createWorkspace(
				userDetails.getPublicId(), request.name(), request.position()
		);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
												  .path("/{id}")
												  .buildAndExpand(workspacePublicId)
												  .toUri();

		return ResponseEntity.created(location)
							 .body(ApiResponse.success("워크스페이스 생성 성공!"));
	}

	/**
	 * 내 워크스페이스 목록 조회
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getMyWorkspaces(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		List<WorkspaceResponse> response = workspaceService.getMyWorkspaces(userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 워크스페이스 상세 조회 (멤버 목록 포함)
	 */
	@GetMapping("/{workspacePublicId}")
	public ResponseEntity<ApiResponse<WorkspaceDetailsResponse>> getWorkspaceDetails(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId
	) {
		WorkspaceDetailsResponse response = workspaceService.getWorkspaceDetails(userDetails.getPublicId(), workspacePublicId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 워크스페이스 이름 수정
	 */
	@PatchMapping("/{workspacePublicId}")
	public ResponseEntity<ApiResponse<Void>> updateWorkspace(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@RequestBody @Valid UpdateWorkspaceRequest request
	) {
		workspaceService.updateWorkspace(userDetails.getPublicId(), workspacePublicId, request.newName());
		return ResponseEntity.ok(ApiResponse.success("워크스페이스 수정 성공!"));
	}

	/**
	 * 워크스페이스 삭제 (Soft Delete)
	 */
	@DeleteMapping("/{workspacePublicId}")
	public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId
	) {
		workspaceService.deleteWorkspace(userDetails.getPublicId(), workspacePublicId);
		return ResponseEntity.ok(ApiResponse.success("워크스페이스 삭제 성공!"));
	}
}
