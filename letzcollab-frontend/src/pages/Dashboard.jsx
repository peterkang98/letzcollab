import { Col, Empty, Flex, Row, Skeleton, Typography } from 'antd';
import { ClockCircleOutlined, ProjectOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from "react-router-dom";
import api from '../api/axios.js';
import ProjectCard from '../components/dashboard/ProjectCard.jsx';
import TaskCard from '../components/dashboard/TaskCard.jsx';
import { useWorkspace } from "../contexts/WorkspaceContext.jsx";
import WorkspaceStats from "../components/dashboard/WorkspaceStats.jsx";
import { timeAgo } from "../utils/dateUtils.js";

const { Title, Text } = Typography;
const PROJECT_POLLING_INTERVAL = 1000 * 60 * 3;
const TASK_POLLING_INTERVAL = 1000 * 60;
const STATS_POLLING_INTERVAL = 1000 * 60 * 5;

export default function Dashboard() {
  const user = JSON.parse(localStorage.getItem('user'));
  const { selectedWorkspaceId: workspaceId } = useWorkspace();
  const nav = useNavigate();

  const { data: projectsData, isLoading: projectsLoading } = useQuery({
    queryKey: ['projects', workspaceId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspaceId}/projects`);
      return res.data.data;
    },
    enabled: !!workspaceId,
    refetchInterval: PROJECT_POLLING_INTERVAL,
    refetchIntervalInBackground: false
  });

  const { data: myTasksData, isLoading: tasksLoading } = useQuery({
    queryKey: ['myTasks', workspaceId],
    queryFn: async () => {
      const res = await api.get('/my/tasks', { params: { workspacePublicId: workspaceId } });
      return res.data.data;
    },
    enabled: !!workspaceId,
    refetchInterval: TASK_POLLING_INTERVAL,
    refetchIntervalInBackground: false
  });

  const { data: statsData, isLoading: statsLoading } = useQuery({
    queryKey: ['workspaceStats', workspaceId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspaceId}/stats`);
      return res.data.data;
    },
    enabled: !!workspaceId,
    refetchInterval: STATS_POLLING_INTERVAL,
    refetchIntervalInBackground: false,
  });

  const projects = projectsData?.content ?? [];
  const totalProjects = projectsData?.page?.totalElements ?? 0;

  const myTasks = myTasksData?.content ?? [];
  const totalMyTasks = myTasksData?.page?.totalElements ?? 0;

  return (
    <Flex vertical gap={24} style={{ padding: '28px 32px', maxWidth: 1200, margin: '0 auto' }}>

      <Flex vertical gap={2}>
        <Text type="secondary" style={{ fontSize: 13 }}>안녕하세요,</Text>
        <Title level={4} style={{ margin: 0 }}>{user?.name}님 👋</Title>
      </Flex>

      {!workspaceId ? (
        <Flex justify="center" align="center" style={{ minHeight: 320 }}>
          <Empty
            image={<ProjectOutlined style={{ fontSize: 48, color: '#d9d9d9' }}/>}
            styles={{ image: { height: 60 } }}
            description={<Text type="secondary">사이드바에서 워크스페이스를 선택하면 대시보드가 표시됩니다</Text>}
          />
        </Flex>
      ) : (
        <Flex vertical gap={24}>
          {/* 워크스페이스 현황 */}
          <Flex vertical gap={16}>
            <Flex justify="space-between" align="center">
              <Text strong style={{ fontSize: 15 }}>워크스페이스 현황</Text>
              <Flex align="center" gap={4}>
                <ClockCircleOutlined style={{ fontSize: 11, color: '#8c8c8c' }} />
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {!statsLoading && timeAgo(statsData.updatedAt)}
                </Text>
              </Flex>
            </Flex>
            <WorkspaceStats data={statsData} isLoading={statsLoading} />
          </Flex>

          {/* 프로젝트 목록 + 내 업무 */}
          <Row gutter={[16, 16]}>

            <Col xs={24} lg={12}>
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
                    {[1, 2, 3].map(i => <Skeleton key={i} active paragraph={{ rows: 2 }}/>)}
                  </Flex>
                ) : !projects.length ? (
                  <Empty description="프로젝트가 없습니다"/>
                ) : (
                  <Flex vertical gap={8}>
                    {projects.map(p => (
                      <div
                        key={p.publicId}
                        style={{ cursor: 'pointer' }}
                        onClick={() => nav(`/workspaces/${workspaceId}/projects/${p.publicId}`)}
                      >
                        <ProjectCard project={p}/>
                      </div>
                    ))}
                  </Flex>
                )}
              </Flex>
            </Col>

            <Col xs={24} lg={12}>
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
                    {[1, 2, 3].map(i => <Skeleton key={i} active paragraph={{ rows: 2 }}/>)}
                  </Flex>
                ) : !myTasks.length ? (
                  <Empty description="담당 업무가 없습니다"/>
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
                        <TaskCard task={t}/>
                      </div>
                    ))}
                  </Flex>
                )}
              </Flex>
            </Col>

          </Row>
        </Flex>
      )}
    </Flex>
  );
}