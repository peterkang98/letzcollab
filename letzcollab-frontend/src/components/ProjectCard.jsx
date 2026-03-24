import { Card, Flex, Space, Tag, Typography } from 'antd';
import { CalendarOutlined, CrownOutlined, LockOutlined, TeamOutlined, UnlockOutlined } from '@ant-design/icons';
import { PROJECT_STATUS_CONFIG } from '../constants/projectStatus.js';

const { Text } = Typography;

export default function ProjectCard({ project }) {
  const status = PROJECT_STATUS_CONFIG[project.status] ?? { color: 'default', label: project.status };

  return (
    <Card size="small" hoverable styles={{ body: { padding: '14px 16px' } }}>
      <Flex vertical gap={8}>

        <Flex justify="space-between" align="flex-start" gap={8}>
          <Flex align="center" gap={6}>
            {project.isPrivate
              ? <LockOutlined style={{ fontSize: 12, color: '#8c8c8c' }}/>
              : <UnlockOutlined style={{ fontSize: 12, color: '#8c8c8c' }}/>
            }
            <Text strong style={{ fontSize: 14 }}>{project.name}</Text>
          </Flex>
          <Tag color={status.color} style={{ margin: 0, flexShrink: 0 }}>{status.label}</Tag>
        </Flex>

        <Flex gap={12}>
          <Space size={4}>
            <CrownOutlined style={{ fontSize: 12, color: '#8c8c8c' }}/>
            <Text type="secondary" style={{ fontSize: 12 }}>{project.leaderName}</Text>
          </Space>
          <Space size={4}>
            <TeamOutlined style={{ fontSize: 12, color: '#8c8c8c' }}/>
            <Text type="secondary" style={{ fontSize: 12 }}>{project.memberCount}명</Text>
          </Space>
        </Flex>

        {(project.startDate || project.endDate) && (
          <Space size={4}>
            <CalendarOutlined style={{ fontSize: 12, color: '#8c8c8c' }}/>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {project.startDate ?? '?'} ~ {project.endDate ?? '?'}
            </Text>
          </Space>
        )}

      </Flex>
    </Card>
  );
}