package xyz.letzcollab.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.project.AddMemberRequest;
import xyz.letzcollab.backend.dto.project.UpdateMyselfRequest;
import xyz.letzcollab.backend.dto.project.UpdateOtherMemberRequest;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.ProjectMemberService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspacePublicId}/projects/{projectPublicId}/members")
public class ProjectMemberController {
	private final ProjectMemberService projectMemberService;

	/**
	 * 프로젝트 멤버 추가 (LEADER / ADMIN만 가능)
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> addMember(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID projectPublicId,
			@RequestBody @Valid AddMemberRequest request
	) {
		projectMemberService.addMember(
				userDetails.getPublicId(), workspacePublicId, projectPublicId, request
		);
		return ResponseEntity.ok(ApiResponse.success("프로젝트 멤버 추가 완료!"));
	}

	/**
	 * 본인 직책 수정
	 */
	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<Void>> updateMyself(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@RequestBody @Valid UpdateMyselfRequest request
	) {
		projectMemberService.updateMyself(userDetails.getPublicId(), projectPublicId, request);
		return ResponseEntity.ok(ApiResponse.success("본인 정보 수정 완료!"));
	}

	/**
	 * 타 멤버 직책/권한 수정 (LEADER / ADMIN만 가능)
	 */
	@PatchMapping("/{targetUserPublicId}")
	public ResponseEntity<ApiResponse<Void>> updateOtherMember(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID targetUserPublicId,
			@RequestBody UpdateOtherMemberRequest request
	) {
		projectMemberService.updateOtherMember(
				userDetails.getPublicId(), workspacePublicId, projectPublicId,
				new UpdateOtherMemberRequest(targetUserPublicId, request.newPosition(), request.newRole())
		);
		return ResponseEntity.ok(ApiResponse.success("프로젝트 멤버 정보/권한 수정 완료!"));
	}

	/**
	 * 프로젝트 자진 탈퇴 (LEADER는 changeLeader 후 탈퇴 가능)
	 */
	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<Void>> leaveProject(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId
	) {
		projectMemberService.leaveProject(userDetails.getPublicId(), projectPublicId);
		return ResponseEntity.ok(ApiResponse.success("프로젝트 탈퇴 완료."));
	}

	/**
	 * 프로젝트 멤버 강퇴 (LEADER / ADMIN만 가능)
	 */
	@DeleteMapping("/{targetUserPublicId}")
	public ResponseEntity<ApiResponse<Void>> kickMember(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID targetUserPublicId
	) {
		projectMemberService.kickMember(userDetails.getPublicId(), workspacePublicId, targetUserPublicId, projectPublicId);
		return ResponseEntity.ok(ApiResponse.success("프로젝트에서 타 멤버 강퇴 완료."));
	}

	/**
	 * 프로젝트 리더 변경 (현재 LEADER만 가능)
	 */
	@PostMapping("/{targetUserPublicId}/change-leader")
	public ResponseEntity<ApiResponse<Void>> changeLeader(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID targetUserPublicId
	) {
		projectMemberService.changeLeader(userDetails.getPublicId(), targetUserPublicId, projectPublicId);
		return ResponseEntity.ok(ApiResponse.success("프로젝트 리더가 변경되었습니다."));
	}
}
