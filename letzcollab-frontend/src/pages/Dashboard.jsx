import { useState } from 'react';
import { Col, Empty, Flex, Row, Skeleton, Typography } from 'antd';
import { ProjectOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios.js';
import WorkspaceSelect from '../components/WorkspaceSelect.jsx';
import ProjectCard from '../components/ProjectCard.jsx';
import TaskCard from '../components/TaskCard.jsx';

const { Title, Text } = Typography;
const POLLING_INTERVAL = 1000 * 60;

export default function Dashboard() {
  const user = JSON.parse(localStorage.getItem('user'));
  const [workspaceId, setWorkspaceId] = useState(null);

  const { data: projects, isLoading: projectsLoading } = useQuery({
    queryKey: ['projects', workspaceId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspaceId}/projects`, { params: { size: 20 } });
      return res.data.data.content;
    },
    enabled: !!workspaceId,
    refetchInterval: POLLING_INTERVAL,
    refetchIntervalInBackground: false
  });

  const { data: myTasks, isLoading: tasksLoading } = useQuery({
    queryKey: ['myTasks', workspaceId],
    queryFn: async () => {
      const res = await api.get('/my/tasks', { params: { workspacePublicId: workspaceId, size: 20 } });
      return res.data.data.content;
    },
    enabled: !!workspaceId,
    refetchInterval: POLLING_INTERVAL,
    refetchIntervalInBackground: false
  });

  return (
    <Flex vertical gap={24} style={{ padding: '28px 32px', maxWidth: 1200, margin: '0 auto' }}>

      <Flex justify="space-between" align="center">
        <Flex vertical gap={2}>
          <Text type="secondary" style={{ fontSize: 13 }}>안녕하세요,</Text>
          <Title level={4} style={{ margin: 0 }}>{user?.name}님 👋</Title>
        </Flex>
        <WorkspaceSelect value={workspaceId} onChange={setWorkspaceId} />
      </Flex>

      {!workspaceId ? (
        <Flex justify="center" align="center" style={{ minHeight: 320 }}>
          <Empty
            image={<ProjectOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />}
            styles={{ image: { height: 60 } }}
            description={<Text type="secondary">워크스페이스를 선택하면 대시보드가 표시됩니다</Text>}
          />
        </Flex>
      ) : (
        <Row gutter={[24, 24]}>

          <Col xs={24} lg={13}>
            <Flex vertical gap={12}>
              <Flex justify="space-between" align="center">
                <Text strong style={{ fontSize: 15 }}>프로젝트</Text>
                {projects && <Text type="secondary" style={{ fontSize: 12 }}>총 {projects.length}개</Text>}
              </Flex>
              {projectsLoading ? (
                <Flex vertical gap={8}>
                  {[1, 2, 3].map(i => <Skeleton key={i} active paragraph={{ rows: 2 }} />)}
                </Flex>
              ) : !projects?.length ? (
                <Empty description="프로젝트가 없습니다" />
              ) : (
                <Flex vertical gap={8}>
                  {projects.map(p => <ProjectCard key={p.publicId} project={p} />)}
                </Flex>
              )}
            </Flex>
          </Col>

          <Col xs={24} lg={11}>
            <Flex vertical gap={12}>
              <Flex justify="space-between" align="center">
                <Text strong style={{ fontSize: 15 }}>내 업무</Text>
                {myTasks && <Text type="secondary" style={{ fontSize: 12 }}>총 {myTasks.length}개</Text>}
              </Flex>
              {tasksLoading ? (
                <Flex vertical gap={8}>
                  {[1, 2, 3].map(i => <Skeleton key={i} active paragraph={{ rows: 2 }} />)}
                </Flex>
              ) : !myTasks?.length ? (
                <Empty description="담당 업무가 없습니다" />
              ) : (
                <Flex vertical gap={8}>
                  {myTasks.map(t => <TaskCard key={t.publicId} task={t} />)}
                </Flex>
              )}
            </Flex>
          </Col>

        </Row>
      )}
    </Flex>
  );
}