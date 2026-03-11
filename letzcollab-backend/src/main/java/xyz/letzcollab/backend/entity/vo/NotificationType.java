package xyz.letzcollab.backend.entity.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
	TASK_ASSIGNED("업무가 할당되었습니다"),
	TASK_STATUS_CHANGED("업무 상태가 변경되었습니다"),
	TASK_REASSIGNED("업무 담당자가 변경되었습니다"),
	TASK_DUE_SOON("업무 마감일이 임박합니다"),
	TASK_OVERDUE("업무가 마감일을 초과했습니다"),
	COMMENT_ADDED("댓글이 달렸습니다"),
	COMMENT_REPLY_ADDED("대댓글이 달렸습니다"),
	PROJECT_MEMBER_ADDED("프로젝트에 초대되었습니다"),
	PROJECT_ROLE_CHANGED("프로젝트에서 권한이 변경되었습니다"),
	PROJECT_MEMBER_REMOVED("프로젝트에서 강퇴되었습니다");

	private final String description;
}