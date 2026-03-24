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
