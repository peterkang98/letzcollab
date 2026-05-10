export const STATUS_CONFIG = {
  TODO: { color: 'default', chartColor: '#f0f0f0', label: '시작 전', order: 4, dtoKey: 'todo' },
  IN_PROGRESS: { color: 'processing', chartColor: '#1677ff', label: '진행 중', order: 2, dtoKey: 'inProgress' },
  IN_REVIEW: { color: 'warning', chartColor: '#faad14', label: '검토 중', order: 3, dtoKey: 'inReview' },
  DONE: { color: 'success', chartColor: '#52c41a', label: '완료', order: 1, dtoKey: 'done' },
  CANCELLED: { color: 'error', chartColor: '#ff4d4f', label: '취소됨', order: 5, dtoKey: 'cancelled' },
};

export const PRIORITY_CONFIG = {
  LOW: { color: 'default', label: '낮음' },
  MEDIUM: { color: 'blue', label: '보통' },
  HIGH: { color: 'orange', label: '높음' },
  URGENT: { color: 'red', label: '긴급' },
};
 