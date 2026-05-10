export const PROJECT_STATUS_CONFIG = {
  PLANNED: { color: 'default', chartColor: '#7F77DD', label: '기획됨', order: 3, dtoKey: 'planned' },
  ACTIVE: { color: 'processing', chartColor: '#1D9E75', label: '진행 중', order: 1, dtoKey: 'active' },
  ON_HOLD: { color: 'warning', chartColor: '#EF9F27', label: '일시 중단', order: 4, dtoKey: 'onHold' },
  COMPLETED: { color: 'success', chartColor: '#639922', label: '완료', order: 2, dtoKey: 'completed' },
  ARCHIVED: { color: '#8c8c8c', chartColor: '#888780', label: '보관됨', order: 5, dtoKey: 'archived' },
};