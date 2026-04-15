package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.workspace.*;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.WorkspaceMemberService;

import java.util.UUID;

@Tag(name = "03-2. Workspace Member", description = "워크스페이스 멤버 초대, 권한 관리 및 탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces")
public class WorkspaceMemberController {
	private final WorkspaceMemberService memberService;

	/**
	 * 멤버 초대 이메일 발송
	 */
	@Operation(summary = "멤버 초대 이메일 발송", description = "피초청인의 이메일로 워크스페이스 초대 링크를 발송합니다. (관리자/소유자 전용)")
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
	@Operation(summary = "초대 수락", description = "이메일로 발송된 토큰을 이용해 워크스페이스 가입 초대를 수락합니다.")
	@PostMapping("/invitations/accept")
	public ResponseEntity<ApiResponse<Void>> acceptInvitation(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid AcceptWorkspaceInvitationRequest request
	) {
		memberService.acceptInvitation(userDetails.getPublicId(), request.token());
		return ResponseEntity.ok(ApiResponse.success("워크스페이스 참여 완료! 이제부터 워크스페이스 멤버로 활동하실 수 있습니다."));
	}

	/**
	 * 본인 정보(권한, 직책) 조회
	 */
	@Operation(summary = "본인 권한/직책 조회", description = "워크스페이스 내에서 자신의 권한 및 직책을 조회합니다.")
	@GetMapping("/{workspacePublicId}/members/me")
	public ResponseEntity<ApiResponse<MyWorkspaceMemberResponse>> getMyself(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID workspacePublicId
	) {
		MyWorkspaceMemberResponse res = memberService.getMyself(workspacePublicId, userDetails.getPublicId());
		return ResponseEntity.ok(ApiResponse.success(res));
	}

	/**
	 * 본인 정보(직책) 수정
	 */
	@Operation(summary = "본인 직책 수정", description = "워크스페이스 내에서 자신의 직책을 수정합니다.")
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
	@Operation(
			summary = "타 멤버 정보 및 권한 수정",
			description = "다른 멤버의 직책과 권한을 수정합니다.\n1. 최소한 관리자 권한 이상 필요\n2. 자신보다 낮은 권한의 멤버만 수정 가능\n3. 내 권한보다 낮은 권한만 부여 가능"
	)
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
	@Operation(summary = "워크스페이스 자진 탈퇴", description = "워크스페이스에서 탈퇴합니다. 워크스페이스 소유자는 소유권 이전 후 탈퇴 가능합니다.")
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
	@Operation(
			summary = "멤버 강퇴",
			description = "특정 멤버를 워크스페이스에서 강퇴합니다.\n1. 최소한 관리자 권한 이상 필요\n2. 자신보다 낮은 권한의 멤버만 강퇴 가능"
	)
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
	@Operation(summary = "소유권 이전", description = "워크스페이스 소유권을 다른 멤버에게 이전합니다.")
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