package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "03-2. Project Member", description = "프로젝트 멤버 추가, 관리 및 강퇴/탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspacePublicId}/projects/{projectPublicId}/members")
public class ProjectMemberController {
	private final ProjectMemberService projectMemberService;

	/**
	 * 프로젝트 멤버 추가 (LEADER / ADMIN만 가능)
	 */
	@Operation(summary = "프로젝트 멤버 추가", description = "프로젝트에 워크스페이스 멤버를 초대합니다. (LEADER 또는 ADMIN만 가능)")
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
	@Operation(summary = "본인 직책 수정", description = "프로젝트 내에서 자신의 직책을 수정합니다.")
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
	@Operation(summary = "타 멤버 권한/직책 수정", description = "다른 멤버의 프로젝트 직책과 권한을 수정합니다. (LEADER/ADMIN 전용)")
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
	@Operation(summary = "프로젝트 자진 탈퇴", description = "프로젝트에서 탈퇴합니다. (리더는 리더 변경 후 탈퇴 가능)")
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
	@Operation(summary = "프로젝트 멤버 강퇴", description = "특정 멤버를 프로젝트에서 제외시킵니다. (LEADER/ADMIN 전용)")
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
	@Operation(summary = "프로젝트 리더 변경", description = "프로젝트의 리더 권한을 다른 멤버에게 위임합니다.")
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
