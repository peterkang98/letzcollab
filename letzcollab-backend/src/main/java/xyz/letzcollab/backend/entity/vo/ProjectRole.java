package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRole {
	ADMIN("관리자"),    // 프로젝트 설정 변경, 멤버 초대/강퇴, 모든 작업 관리
	MEMBER("참여자"),   // 작업 생성, 수정, 완료 처리 등 실무 수행
	VIEWER("조회자");    // 프로젝트 진행 상황 열람만 가능 (+ 댓글 허용)

	private final String description;
}
