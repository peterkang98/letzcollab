export const NOTIFICATION_TYPE_CONFIG = {
  TASK_ASSIGNED: { label: '업무 할당', color: 'blue' },
  TASK_STATUS_CHANGED: { label: '상태 변경', color: 'cyan' },
  TASK_REASSIGNED: { label: '담당자 변경', color: 'geekblue' },
  TASK_DUE_SOON: { label: '마감 임박', color: 'orange' },
  TASK_OVERDUE: { label: '기한 초과', color: 'red' },
  COMMENT_ADDED: { label: '댓글', color: 'green' },
  COMMENT_REPLY_ADDED: { label: '대댓글', color: 'lime' },
  PROJECT_MEMBER_ADDED: { label: '프로젝트 초대', color: 'purple' },
  PROJECT_ROLE_CHANGED: { label: '권한 변경', color: 'volcano' },
};

export function buildNotifLink(notif) {
  if (notif.referenceType === 'TASK' && notif.parentReferenceId) {
    return `/projects/${notif.parentReferenceId}/tasks/${notif.referenceId}`;
  }
  if (notif.referenceType === 'PROJECT' && notif.referenceId && notif.parentReferenceId) {
    return `/workspaces/${notif.parentReferenceId}/projects/${notif.referenceId}`;
  }

  return null;
}
