package xyz.letzcollab.backend.controller;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects/{projectPublicId}/tasks/{taskPublicId}/comments")
public class TaskCommentController {

	private final TaskCommentService taskCommentService;

	/** 댓글/대댓글 생성 */
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


	@PatchMapping("/{commentId}")
	public ResponseEntity<ApiResponse<Void>> updateComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long commentId,
			@RequestBody @Valid UpdateCommentRequest request
	) {
		taskCommentService.updateComment(userDetails.getPublicId(), commentId, request.newContent());
		return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공!"));
	}


	@DeleteMapping("/{commentId}")
	public ResponseEntity<ApiResponse<Void>> deleteComment(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long commentId
	) {
		taskCommentService.deleteComment(userDetails.getPublicId(), commentId);
		return ResponseEntity.ok(ApiResponse.success("댓글 삭제 성공!"));
	}
}