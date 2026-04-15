import { useState } from 'react';
import { Avatar, Badge, Card, Flex, message, Tag, Typography } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { DragDropProvider, useDraggable, useDroppable } from '@dnd-kit/react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PRIORITY_CONFIG, STATUS_CONFIG } from '../../constants/taskStatus.js';
import { formatDueDate } from '../../utils/dateUtils.js';

const { Text } = Typography;

const COLUMN_DOT_COLOR = {
  TODO: '#8c8c8c',
  IN_PROGRESS: '#1677ff',
  IN_REVIEW: '#fa8c16',
  DONE: '#52c41a',
  CANCELLED: '#ff4d4f',
};

const COLUMNS = Object.entries(STATUS_CONFIG).map(([key, cfg]) => ({ key, label: cfg.label }));

export default function KanbanBoard({ tasks, projectPublicId }) {
  const queryClient = useQueryClient();
  const [activeTask, setActiveTask] = useState(null);
  const [overColumnId, setOverColumnId] = useState(null); // 드래그 중 hover된 컬럼

  // O(N)
  const grouped = tasks.reduce((acc, task) => {
    if (!acc[task.status]) acc[task.status] = [];
    acc[task.status].push(task);
    return acc;
  }, {});

  const updateStatusMutation = useMutation({
    mutationFn: ({ taskPublicId, newStatus }) =>
      api.patch(`/projects/${projectPublicId}/tasks/${taskPublicId}`, { status: newStatus }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectPublicId] });
    },
    onError: (e) => {
      const errorCode = e.response?.data?.errorCode;
      if (errorCode === 'C007') {
        message.error('권한이 부족합니다.');
      } else {
        message.error(e.response?.data?.message || '상태 변경에 실패했습니다.');
      }
      queryClient.invalidateQueries({ queryKey: ['tasks', projectPublicId] });
    },
  });

  const handleDragOver = (event) => {
    const targetId = event.operation.target?.id;
    const isColumn = COLUMNS.some(col => col.key === targetId);
    setOverColumnId(isColumn ? targetId : null);
  };

  const handleDragEnd = (event) => {
    setActiveTask(null);
    setOverColumnId(null);
    if (event.canceled) return;

    const draggedTaskId = event.operation.source?.id;
    const targetColumnId = event.operation.target?.id;
    if (!draggedTaskId || !targetColumnId) return;

    const draggedTask = tasks.find(t => t.publicId === draggedTaskId);
    if (!draggedTask) return;

    const isColumn = COLUMNS.some(col => col.key === targetColumnId);
    if (!isColumn) return;
    if (targetColumnId === draggedTask.status) return;

    updateStatusMutation.mutate({ taskPublicId: draggedTask.publicId, newStatus: targetColumnId });
  };

  return (
    <DragDropProvider
      onDragStart={(event) => {
        setActiveTask(tasks.find(t => t.publicId === event.operation.source?.id) ?? null);
      }}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
    >
      <div style={{ display: 'flex', gap: 16, overflowX: 'auto', paddingBottom: 8, alignItems: 'flex-start' }}>
        {COLUMNS.map(col => (
          <KanbanColumn
            key={col.key}
            colKey={col.key}
            label={col.label}
            dotColor={COLUMN_DOT_COLOR[col.key] ?? '#8c8c8c'}
            tasks={grouped[col.key] ?? []}
            projectPublicId={projectPublicId}
            activeTaskId={activeTask?.publicId}
            isOver={overColumnId === col.key}
          />
        ))}
      </div>
    </DragDropProvider>
  );
}

function KanbanColumn({ colKey, label, dotColor, tasks, projectPublicId, activeTaskId, isOver }) {
  const { ref } = useDroppable({ id: colKey });

  return (
    <div
      ref={ref}
      style={{
        minWidth: 260,
        flex: '0 0 260px',
        background: isOver ? 'rgba(22,119,255,0.08)' : '#f5f5f5',
        border: isOver ? '2px solid #1677ff' : '2px solid transparent',
        borderRadius: 12,
        padding: '12px 10px',
        transition: 'background 0.15s, border-color 0.15s',
      }}
    >
      <Flex align="center" gap={8} style={{ marginBottom: 12, padding: '0 4px' }}>
        <div style={{ width: 10, height: 10, borderRadius: '50%', background: dotColor, flexShrink: 0 }} />
        <Text strong style={{ fontSize: 14, flex: 1 }}>{label}</Text>
        <Badge count={tasks.length} showZero
               style={{ background: '#d9d9d9', color: '#595959', boxShadow: 'none' }} />
      </Flex>

      <Flex vertical gap={8}>
        {tasks.map(task => (
          <DraggableTaskCard
            key={task.publicId}
            task={task}
            projectPublicId={projectPublicId}
            isActiveDrag={task.publicId === activeTaskId}
          />
        ))}
        {tasks.length === 0 && (
          <div style={{ padding: '16px 0', textAlign: 'center' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>없음</Text>
          </div>
        )}
      </Flex>
    </div>
  );
}

function DraggableTaskCard({ task, projectPublicId, isActiveDrag }) {
  const nav = useNavigate();
  const { ref } = useDraggable({ id: task.publicId });

  return (
    <div ref={ref} style={{ opacity: isActiveDrag ? 0.4 : 1, cursor: 'grab' }}>
      <TaskKanbanCard
        task={task}
        onClick={() => nav(`/projects/${projectPublicId}/tasks/${task.publicId}`)}
      />
    </div>
  );
}

function TaskKanbanCard({ task, onClick }) {
  const priority = PRIORITY_CONFIG[task.priority] ?? { color: 'default', label: task.priority };
  const due = formatDueDate(task.dueDate);

  return (
    <Card
      hoverable
      onClick={onClick}
      styles={{ body: { padding: '14px 16px' } }}
      style={{ borderRadius: 10, background: '#fff', border: '1px solid #f0f0f0' }}
    >
      <Flex vertical gap={10}>
        <Text strong style={{ fontSize: 14, wordBreak: 'break-word', lineHeight: 1.4 }}>
          {task.name}
        </Text>
        {task.description && (
          <Text type="secondary" style={{ fontSize: 12, lineHeight: 1.4,
            overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical' }}>
            {task.description}
          </Text>
        )}
        <Flex justify="space-between" align="center">
          <Flex align="center" gap={6}>
            <Tag color={priority.color} style={{ margin: 0, fontSize: 11, padding: '0 6px' }}>
              {priority.label}
            </Tag>
            {due && (
              <Text style={{ fontSize: 11, color: due.danger ? '#ff4d4f' : '#8c8c8c' }}>
                {due.text}
              </Text>
            )}
          </Flex>
          <Avatar size={24} icon={<UserOutlined />}
                  style={{ background: '#1677ff', fontSize: 11, flexShrink: 0 }}>
            {task.assigneeName?.[0]}
          </Avatar>
        </Flex>
      </Flex>
    </Card>
  );
}