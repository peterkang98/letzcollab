// ProjectRole enum 매핑
export const PROJECT_ROLE_CONFIG = {
  ADMIN: { label: '관리자', color: 'blue' },
  MEMBER: { label: '참여자', color: 'green' },
  VIEWER: { label: '조회자', color: 'default' },
};

// Select 옵션용
export const PROJECT_ROLE_OPTIONS = Object.entries(PROJECT_ROLE_CONFIG).map(([value, cfg]) => ({
  value,
  label: cfg.label,
}));