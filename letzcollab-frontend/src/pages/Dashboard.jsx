import { useState } from 'react';
import { Col, Empty, Flex, Row, Skeleton, Typography } from 'antd';
import { ProjectOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from "react-router-dom";
import api from '../api/axios.js';
import WorkspaceSelect from '../components/dashboard/WorkspaceSelect.jsx';
import ProjectCard from '../components/dashboard/ProjectCard.jsx';
import TaskCard from '../components/dashboard/TaskCard.jsx';

const { Title, Text } = Typography;
const POLLING_INTERVAL = 1000 * 60;

export default function Dashboard() {
  const user = JSON.parse(localStorage.getItem('user'));
  const [workspaceId, setWorkspaceId] = useState(null);
  const nav = useNavigate();

  const { data: projectsData, isLoading: projectsLoading } = useQuery({
    queryKey: ['projects', workspaceId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspaceId}/projects`);
      return res.data.data;
    },
    enabled: !!workspaceId,
    refetchInterval: POLLING_INTERVAL,
    refetchIntervalInBackground: false
  });

  const { data: myTasksData, isLoading: tasksLoading } = useQuery({
    queryKey: ['myTasks', workspaceId],
    queryFn: async () => {
      const res = await api.get('/my/tasks', { params: { workspacePublicId: workspaceId } });
      return res.data.data;
    },
    enabled: !!workspaceId,
    refetchInterval: POLLING_INTERVAL,
    refetchIntervalInBackground: false
  });

  const projects = projectsData?.content ?? [];
  const totalProjects = projectsData?.page?.totalElements ?? 0;

  const myTasks = myTasksData?.content ?? [];
  const totalMyTasks = myTasksData?.page?.totalElements ?? 0;

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
                {/* totalElements로 전체 개수 표시, 20개 초과 시 "최근 20개" 안내 */}
                {projectsData && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    총 {totalProjects}개
                    {totalProjects > 20 && ' (최근 20개)'}
                  </Text>
                )}
              </Flex>
              {projectsLoading ? (
                <Flex vertical gap={8}>
                  {[1, 2, 3].map(i => <Skeleton key={i} active paragraph={{ rows: 2 }} />)}
                </Flex>
              ) : !projects.length ? (
                <Empty description="프로젝트가 없습니다" />
              ) : (
                <Flex vertical gap={8}>
                  {projects.map(p => (
                    <div
                      key={p.publicId}
                      style={{ cursor: 'pointer' }}
                      onClick={() => nav(`/workspaces/${workspaceId}/projects/${p.publicId}`)}
                    >
                      <ProjectCard project={p} />
                    </div>
                  ))}
                </Flex>
              )}
            </Flex>
          </Col>

          <Col xs={24} lg={11}>
            <Flex vertical gap={12}>
              <Flex justify="space-between" align="center">
                <Text strong style={{ fontSize: 15 }}>내 업무</Text>
                {myTasksData && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    총 {totalMyTasks}개
                    {totalMyTasks > 20 && ' (마감 임박순 20개)'}
                  </Text>
                )}
              </Flex>
              {tasksLoading ? (
                <Flex vertical gap={8}>
                  {[1, 2, 3].map(i => <Skeleton key={i} active paragraph={{ rows: 2 }} />)}
                </Flex>
              ) : !myTasks.length ? (
                <Empty description="담당 업무가 없습니다" />
              ) : (
                <Flex vertical gap={8}>
                  {myTasks.map(t => (
                    <div
                      key={t.publicId}
                      style={{ cursor: 'pointer' }}
                      onClick={() => t.projectPublicId
                        ? nav(`/projects/${t.projectPublicId}/tasks/${t.publicId}`)
                        : undefined
                      }
                    >
                      <TaskCard task={t} />
                    </div>
                  ))}
                </Flex>
              )}
            </Flex>
          </Col>

        </Row>
      )}
    </Flex>
  );
}