import { Card, Flex, Tag, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { PRIORITY_CONFIG, STATUS_CONFIG } from '../../constants/taskStatus.js';
import { formatDueDate } from '../../utils/dateUtils.js';

const { Text } = Typography;

export default function SubTaskCard({ subTask, projectPublicId }) {
  const nav = useNavigate();
  const status = STATUS_CONFIG[subTask.status] ?? { color: 'default', label: subTask.status };
  const priority = PRIORITY_CONFIG[subTask.priority] ?? { color: 'default', label: subTask.priority };
  const due = formatDueDate(subTask.dueDate);

  return (
    <Card
      size="small"
      hoverable
      onClick={() => nav(`/projects/${projectPublicId}/tasks/${subTask.publicId}`)}
      styles={{ body: { padding: '10px 14px' } }}
    >
      <Flex justify="space-between" align="center" gap={8}>
        <Flex align="center" gap={8} style={{ minWidth: 0 }}>
          <Tag color={priority.color} style={{ margin: 0, flexShrink: 0 }}>{priority.label}</Tag>
          <Text ellipsis style={{ fontSize: 13 }}>{subTask.name}</Text>
        </Flex>
        <Flex align="center" gap={8} style={{ flexShrink: 0 }}>
          {due && (
            <Text style={{ fontSize: 11 }} type={due.danger ? 'danger' : 'secondary'}>
              {due.text}
            </Text>
          )}
          <Tag color={status.color} style={{ margin: 0 }}>{status.label}</Tag>
        </Flex>
      </Flex>
    </Card>
  );
}