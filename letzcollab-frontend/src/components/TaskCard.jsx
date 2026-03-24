import { Card, Flex, Space, Tag, Typography } from 'antd';
import { FolderOutlined } from '@ant-design/icons';
import { PRIORITY_CONFIG, STATUS_CONFIG } from '../constants/taskStatus.js';
import { formatDueDate } from '../utils/dateUtils.js';

const { Text } = Typography;

export default function TaskCard({ task }) {
  const status = STATUS_CONFIG[task.status] ?? { color: 'default', label: task.status };
  const priority = PRIORITY_CONFIG[task.priority] ?? { color: 'default', label: task.priority };
  const due = formatDueDate(task.dueDate);

  return (
    <Card size="small" hoverable styles={{ body: { padding: '12px 16px' } }}>
      <Flex vertical gap={6}>

        <Flex justify="space-between" align="center">
          <Tag color={priority.color} style={{ margin: 0 }}>{priority.label}</Tag>
          <Tag color={status.color} style={{ margin: 0 }}>{status.label}</Tag>
        </Flex>

        <Text strong style={{ fontSize: 13 }}>{task.name}</Text>

        <Flex justify="space-between" align="center">
          <Space size={4}>
            <FolderOutlined style={{ fontSize: 11, color: '#8c8c8c' }}/>
            <Text type="secondary" style={{ fontSize: 11 }}>{task.projectName}</Text>
          </Space>
          {due && (
            <Text style={{ fontSize: 11 }} type={due.danger ? 'danger' : 'secondary'}>
              {due.text}
            </Text>
          )}
        </Flex>

      </Flex>
    </Card>
  );
}