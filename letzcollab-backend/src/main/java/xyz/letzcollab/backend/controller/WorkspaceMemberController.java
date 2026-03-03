package xyz.letzcollab.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.workspace.AcceptWorkspaceInvitationRequest;
import xyz.letzcollab.backend.dto.workspace.MemberUpdateMyselfRequest;
import xyz.letzcollab.backend.dto.workspace.MemberUpdateOtherRequest;
import xyz.letzcollab.backend.dto.workspace.WorkspaceInviteRequest;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.WorkspaceMemberService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceMemberController {
	private final WorkspaceMemberService memberService;

	/**
	 * 멤버 초대 이메일 발송
	 */
	@PostMapping("/{workspacePublicId}/invitations")
	public ResponseEntity<ApiResponse<Void>> inviteMember(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@RequestBody @Valid WorkspaceInviteRequest request
	) {
		memberService.inviteMemberByEmail(
				userDetails.getPublicId(), workspacePublicId, request.email(), request.position()
		);
		return ResponseEntity.ok(ApiResponse.success("초대 이메일 발송 성공! 피초청인이 24시간 이내에 수락해야 합니다."));
	}

	/**
	 * 초대 수락
	 */
	@PostMapping("/invitations/accept")
	public ResponseEntity<ApiResponse<Void>> acceptInvitation(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid AcceptWorkspaceInvitationRequest request
	) {
		memberService.acceptInvitation(userDetails.getPublicId(), request.token());
		return ResponseEntity.ok(ApiResponse.success("워크스페이스 참여 완료! 이제부터 워크스페이스 멤버로 활동하실 수 있습니다."));
	}

	/**
	 * 본인 정보(직책) 수정
	 */
	@PatchMapping("/{workspacePublicId}/members/me")
	public ResponseEntity<ApiResponse<Void>> updateMyself(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@RequestBody @Valid MemberUpdateMyselfRequest request
	) {
		memberService.updateMyself(workspacePublicId, userDetails.getPublicId(), request.position());
		return ResponseEntity.ok(ApiResponse.success("본인 정보 수정 완료!"));
	}

	/**
	 * 타 멤버 정보/권한 수정
	 */
	@PatchMapping("/{workspacePublicId}/members/{memberPublicId}")
	public ResponseEntity<ApiResponse<Void>> updateOtherMember(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID memberPublicId,
			@RequestBody @Valid MemberUpdateOtherRequest request
	) {
		memberService.updateOtherMember(
				userDetails.getPublicId(), workspacePublicId, memberPublicId,
				request.position(), request.role()
		);
		return ResponseEntity.ok(ApiResponse.success("타 멤버의 정보/권한 수정 완료!"));
	}

	/**
	 * 워크스페이스 자진 탈퇴
	 */
	@DeleteMapping("/{workspacePublicId}/members/me")
	public ResponseEntity<ApiResponse<Void>> leaveWorkspace(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId
	) {
		memberService.leaveWorkspace(userDetails.getPublicId(), workspacePublicId);
		return ResponseEntity.ok(ApiResponse.success("워크스페이스 탈퇴 완료."));
	}

	/**
	 * 멤버 강퇴
	 */
	@DeleteMapping("/{workspacePublicId}/members/{memberPublicId}")
	public ResponseEntity<ApiResponse<Void>> kickMember(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID memberPublicId
	) {
		memberService.kickMember(userDetails.getPublicId(), workspacePublicId, memberPublicId);
		return ResponseEntity.ok(ApiResponse.success("워크스페이스에서 타 멤버 강퇴 완료."));
	}

	/**
	 * 소유권 이전
	 */
	@PostMapping("/{workspacePublicId}/members/{memberPublicId}/transfer-ownership")
	public ResponseEntity<ApiResponse<Void>> transferOwnership(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId,
			@PathVariable UUID memberPublicId
	) {
		memberService.transferOwnership(userDetails.getPublicId(), workspacePublicId, memberPublicId);
		return ResponseEntity.ok(ApiResponse.success("워크스페이스의 소유권이 이전 되었습니다."));
	}
}