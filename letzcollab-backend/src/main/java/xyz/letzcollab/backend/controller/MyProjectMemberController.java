package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.project.MyProjectMemberResponse;
import xyz.letzcollab.backend.dto.project.UpdateMyselfRequest;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.ProjectMemberService;

import java.util.UUID;

@Tag(name = "04-3. My Project Member", description = "프로젝트 내 내 멤버 정보 조회/수정/탈퇴 및 리더 변경 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/projects/{projectPublicId}/members")
public class MyProjectMemberController {
	private final ProjectMemberService projectMemberService;

	/**
	 * 내 프로젝트 멤버 정보 조회 (role, position)
	 */
	@Operation(summary = "내 프로젝트 멤버 정보 조회", description = "현재 로그인한 사용자의 해당 프로젝트 내 역할(role)과 직책(position)을 조회합니다.")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<MyProjectMemberResponse>> getMyMemberInfo(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId
	) {
		MyProjectMemberResponse response = projectMemberService.getMyMemberInfo(
				userDetails.getPublicId(), projectPublicId
		);
		return ResponseEntity.ok(ApiResponse.success(response));
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
