package xyz.letzcollab.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.letzcollab.backend.dto.comment.CommentResponse;
import xyz.letzcollab.backend.dto.comment.CreateCommentRequest;
import xyz.letzcollab.backend.dto.comment.UpdateCommentRequest;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.TaskCommentService;

import java.util.List;
import java.util.UUID;

@Tag(name = "5. Task Comment", description = "업무 댓글 및 대댓글 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectPublicId}/tasks/{taskPublicId}/comments")
public class TaskCommentController {

	private final TaskCommentService taskCommentService;

	/** 댓글/대댓글 생성 */
	@Operation(summary = "댓글 및 대댓글 생성", description = "parentCommentId 유무에 따라 최상위 댓글 또는 대댓글을 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID taskPublicId,
			@RequestBody @Valid CreateCommentRequest request
	) {
		taskCommentService.createComment(
				userDetails.getPublicId(), projectPublicId, taskPublicId, request.content(), request.parentCommentId()
		);
		return ResponseEntity.ok(ApiResponse.success("댓글 작성 성공!"));
	}

	/** 댓글 목록 조회 */
	@Operation(summary = "댓글 목록 조회", description = "해당 업무에 작성된 모든 댓글과 대댓글을 계층형으로 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable UUID projectPublicId,
			@PathVariable UUID taskPublicId
	) {
		List<CommentResponse> response = taskCommentService.getComments(
				userDetails.getPublicId(), projectPublicId, taskPublicId
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 수정", description = "자신이 작성한 댓글/대댓글의 내용을 수정합니다.")
	@PatchMapping("/{commentId}")
	public ResponseEntity<ApiResponse<Void>> updateComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long commentId,
			@RequestBody @Valid UpdateCommentRequest request
	) {
		taskCommentService.updateComment(userDetails.getPublicId(), commentId, request.newContent());
		return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공!"));
	}

	@Operation(summary = "댓글 삭제", description = "자신이 작성한 댓글을 삭제합니다. (최상위 댓글 삭제 시 대댓글도 연쇄 삭제됨)")
	@DeleteMapping("/{commentId}")
	public ResponseEntity<ApiResponse<Void>> deleteComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long commentId
	) {
		taskCommentService.deleteComment(userDetails.getPublicId(), commentId);
		return ResponseEntity.ok(ApiResponse.success("댓글 삭제 성공!"));
	}
}