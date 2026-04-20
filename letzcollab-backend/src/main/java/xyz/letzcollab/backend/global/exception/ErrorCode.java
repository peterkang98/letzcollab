package xyz.letzcollab.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// --- Common (C) ---
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
	INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "데이터 타입이 올바르지 않습니다."),
	INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "C006", "잘못된 형식의 JSON 요청입니다."),
	INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "C007", "해당 작업을 수행할 권한이 없습니다."),

	// --- Auth (A) ---
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증되지 않은 사용자입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
	JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "만료된 JWT 토큰입니다."),
	JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 JWT 토큰입니다."),
	DISABLED(HttpStatus.FORBIDDEN, "A005", "이메일 인증을 먼저 완료해주세요."),
	LOCKED(HttpStatus.FORBIDDEN, "A006", "탈퇴했거나 차단된 계정입니다."),
	BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A007", "이메일 또는 비밀번호가 잘못되었습니다"),

	// --- User (U) ---
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 가입된 이메일입니다."),
	USER_ALREADY_LOGGED_IN(HttpStatus.BAD_REQUEST, "U003", "이미 로그인된 상태입니다."),

	// --- Email (E) ---
	EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "이메일 발송을 실패했습니다."),
	VERIFICATION_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "해당 인증 토큰을 찾을 수 없습니다."),
	VERIFICATION_TOKEN_EXPIRED(HttpStatus.GONE, "E003", "인증 토큰이 만료되었습니다."),
	VERIFICATION_TOKEN_ALREADY_USED(HttpStatus.GONE, "E004", "이미 사용된 인증 토큰입니다."),
	EMAIL_SEND_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "E005", "이메일 발송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	VERIFICATION_TOKEN_NOT_EXPIRED(HttpStatus.BAD_REQUEST, "E006", "아직 유효한 인증 토큰이 있습니다. 이메일을 확인해주세요."),

	// --- Workspace (W) ---
	// 보안을 위해, 권한 미달 때문에 실패했는지, 리소스가 없어서 실패했는지 정확히 알려주지 않음
	WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED(HttpStatus.NOT_FOUND, "W001", "워크스페이스가 존재하지 않거나 접근 권한이 없습니다."),
	WORKSPACE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "W002", "워크스페이스에서 해당 사용자를 찾을 수 없습니다."),
	ALREADY_A_WORKSPACE_MEMBER(HttpStatus.CONFLICT, "W003", "이미 워크스페이스에 가입된 사용자입니다."),

	WORKSPACE_INVITATION_INVALID(HttpStatus.GONE, "W004", "만료되거나 유효하지 않은 초대장입니다."),
	WORKSPACE_INVITEE_MISMATCH(HttpStatus.FORBIDDEN, "W005", "초대받은 계정으로 로그인해주세요."),

	DUPLICATE_WORKSPACE_NAME(HttpStatus.CONFLICT, "W006", "이미 사용 중인 워크스페이스 이름입니다."),
	WORKSPACE_OWNER_RELEASE_REQUIRED(HttpStatus.BAD_REQUEST, "W007", "소유권을 이전하기 전에는 워크스페이스를 떠날 수 없습니다."),
	CANNOT_TRANSFER_OWNERSHIP_TO_SELF(HttpStatus.BAD_REQUEST, "W008", "소유권을 자기 자신에게 이전할 수 없습니다."),
	USE_SELF_UPDATE_API(HttpStatus.BAD_REQUEST, "W009", "본인 정보 수정은 전용 메뉴를 이용해 주세요."),
	USE_SELF_DELETE_API(HttpStatus.BAD_REQUEST, "W010", "본인 계정 탈퇴는 전용 메뉴를 이용해 주세요."),

	// --- Project (P) ---
	INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "P001", "프로젝트 종료일은 시작일보다 빠를 수 없습니다."),
	DUPLICATE_PROJECT_NAME(HttpStatus.CONFLICT, "P002", "이미 사용 중인 프로젝트 이름입니다."),
	PROJECT_NOT_FOUND_OR_ACCESS_DENIED(HttpStatus.NOT_FOUND, "P003", "프로젝트가 존재하지 않거나 접근 권한이 없습니다."),
	ALREADY_A_PROJECT_MEMBER(HttpStatus.CONFLICT, "P004", "이미 프로젝트 멤버입니다."),
	PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "프로젝트에서 해당 사용자를 찾을 수 없습니다."),
	PROJECT_LEADER_RELEASE_REQUIRED(HttpStatus.BAD_REQUEST, "P006", "리더를 변경한 후에 프로젝트를 떠날 수 있습니다."),
	CANNOT_CHANGE_LEADER_TO_SELF(HttpStatus.BAD_REQUEST, "P007", "리더 권한을 자기 자신에게 이전할 수 없습니다."),

	// --- Task (T) ---
	TASK_NOT_FOUND_OR_ACCESS_DENIED(HttpStatus.NOT_FOUND, "T001", "업무가 존재하지 않거나 접근 권한이 없습니다."),
	CANNOT_ASSIGN_TASK_TO_VIEWER(HttpStatus.BAD_REQUEST, "T002", "업무를 조회자에게 할당할 수 없습니다."),

	// --- Task Comment (TC) ---
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "TC001", "댓글을 찾을 수 없습니다."),
	COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "TC002", "댓글 작성자만 수정/삭제할 수 있습니다."),
	COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "TC003", "대댓글에는 답글을 달 수 없습니다."),

	// --- Notification (N) ---
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
