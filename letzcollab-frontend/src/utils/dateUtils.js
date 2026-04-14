// 마감일을 오늘 기준으로 포맷
export function formatDueDate(dueDate) {
  if (!dueDate) return null;

  const today = new Date().setHours(0, 0, 0, 0);
  const due = new Date(dueDate).setHours(0, 0, 0, 0);
  const diff = Math.round((due - today) / (1000 * 60 * 60 * 24));

  const isUrgent = diff <= 3;

  if (diff < 0)  return { text: `${Math.abs(diff)}일 초과`, danger: true };
  if (diff === 0) return { text: '오늘 마감', danger: true };
  
  return { text: `D-${diff}`, danger: isUrgent };
}

// 생성 시각을 현재 기준 상대 시간으로 포맷
export function timeAgo(dateStr) {
  if (!dateStr) return '';

  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);

  if (mins < 1)   return '방금 전';
  if (mins < 60)  return `${mins}분 전`;

  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}시간 전`;

  const days = Math.floor(hours / 24);
  if (days < 30)  return `${days}일 전`;

  const months = Math.floor(days / 30);
  if (months < 12) return `${months}달 전`;

  return `${Math.floor(months / 12)}년 전`;
}

// LocalDateTime 문자열 → "YYYY.MM.DD HH:mm" 포맷
export function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}