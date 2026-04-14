// WorkspaceRole enum 매핑 (백엔드: OWNER > ADMIN > MEMBER > GUEST)
export const WORKSPACE_ROLE_CONFIG = {
  OWNER: { label: '소유자', color: 'gold', level: 3 },
  ADMIN: { label: '관리자', color: 'blue', level: 2 },
  MEMBER: { label: '일반 멤버', color: 'green', level: 1 },
  GUEST: { label: '외부 협력자', color: 'default', level: 0 },
};

// 역할 수준 비교 헬퍼
export const getRoleLevel = (role) => WORKSPACE_ROLE_CONFIG[role]?.level ?? -1;

// 내 역할이 대상보다 높은지 확인 (강퇴/수정 가능 여부)
export const canManage = (myRole, targetRole) =>
  getRoleLevel(myRole) > getRoleLevel(targetRole);

// 초대/강퇴가 가능한 역할인지 확인 (OWNER, ADMIN만 가능)
export const canInvite = (myRole) => getRoleLevel(myRole) >= 2;

// 내가 부여할 수 있는 역할 목록 (내 역할보다 낮은 것만)
export const getAssignableRoles = (myRole) =>
  Object.entries(WORKSPACE_ROLE_CONFIG)
    .filter(([, cfg]) => cfg.level < getRoleLevel(myRole))
    .map(([key, cfg]) => ({ value: key, label: cfg.label }));