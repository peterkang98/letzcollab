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
import xyz.letzcollab.backend.dto.task.MyTaskResponse;
import xyz.letzcollab.backend.dto.task.MyTaskSearchCond;
import xyz.letzcollab.backend.global.dto.ApiResponse;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;
import xyz.letzcollab.backend.service.MyService;

@Tag(name = "07. My", description = "로그인한 사용자 기준의 통합 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/my")
public class MyController {

	private final MyService myService;

	@Operation(
			summary = "내 업무 전체 조회",
			description = "내가 담당자(assignee)로 지정된 모든 업무를 조회합니다. 상태, 우선순위, 마감일 범위로 필터링 가능합니다."
	)
	@GetMapping("/tasks")
	public ResponseEntity<ApiResponse<Page<MyTaskResponse>>> getMyTasks(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody @Valid MyTaskSearchCond cond,
			Pageable pageable
	) {
		Page<MyTaskResponse> response = myService.getMyTasks(userDetails.getPublicId(), cond, pageable);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}