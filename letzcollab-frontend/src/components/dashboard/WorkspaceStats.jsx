import { Card, Col, Flex, Progress, Row, Skeleton, Typography } from 'antd';
import { AlertOutlined, CheckCircleOutlined, FolderOutlined, TeamOutlined, } from '@ant-design/icons';
import StatCard from './StatCard.jsx';
import TaskDistributionBar from './TaskDistributionBar.jsx';
import ProjectStatusList from './ProjectStatusList.jsx';

const { Text } = Typography;

export default function WorkspaceStats({ data, isLoading }) {
  if (isLoading) {
    return (
      <Row gutter={[16, 16]}>
        {[1, 2, 3, 4].map(i => (
          <Col key={i} xs={12} sm={12} md={6}>
            <Skeleton active paragraph={{ rows: 1 }} />
          </Col>
        ))}
      </Row>
    );
  }

  if (!data) return null;

  const { tasks: t = {}, projects: p = {}, totalMembers = 0 } = data;

  return (
    <Flex vertical gap={16}>

      {/* 상단 요약 카드 */}
      <Row gutter={[16, 16]}>
        <Col xs={12} sm={12} md={6}>
          <StatCard
            icon={<FolderOutlined />}
            value={p.active ?? 0}
            label="진행 중인 프로젝트"
            color="#1677ff"
          />
        </Col>

        <Col xs={12} sm={12} md={6}>
          <StatCard
            icon={<TeamOutlined />}
            value={totalMembers}
            label="전체 멤버"
            color="#722ed1"
          />
        </Col>

        <Col xs={12} sm={12} md={6}>
          <StatCard
            icon={<CheckCircleOutlined />}
            value={t.completionRate ?? 0}
            suffix="%"
            label="업무 완료율"
            color="#13c2c2"
          />
        </Col>

        <Col xs={12} sm={12} md={6}>
          <StatCard
            icon={<AlertOutlined />}
            value={t.overdue ?? 0}
            label="마감 초과 업무"
            color={t.overdue > 0 ? '#ff4d4f' : '#8c8c8c'}
          />
        </Col>
      </Row>

      {/* 하단 현황 카드 */}
      <Row gutter={[16, 16]}>

        {/* 업무 현황 */}
        <Col xs={24} md={12}>
          <Card
            title="업무 현황"
            variant={"borderless"}
            extra={
              <Text type="secondary" style={{ fontSize: 12 }}>
                전체 {t.total ?? 0}개
              </Text>
            }
            style={{ height: '100%' }}
            styles={{
              header: {
                minHeight: 52,
              },
              body: {
                paddingTop: 16,
              },
            }}
          >
            <Flex vertical gap={20}>

              {/* 완료율 */}
              <Flex vertical gap={6}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  완료율
                </Text>

                <Progress
                  percent={t.completionRate ?? 0}
                  strokeColor="#13c2c2"
                  railColor="#f0f0f0"
                />
              </Flex>

              {/* 상태 분포 */}
              <Flex vertical gap={6}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  상태별 분포
                </Text>

                <TaskDistributionBar
                  stats={{
                    todo: t.todo ?? 0,
                    inProgress: t.inProgress ?? 0,
                    inReview: t.inReview ?? 0,
                    done: t.done ?? 0,
                    cancelled: t.cancelled ?? 0,
                  }}
                />
              </Flex>

            </Flex>
          </Card>
        </Col>

        {/* 프로젝트 현황 */}
        <Col xs={24} md={12}>
          <Card
            title="프로젝트 현황"
            variant={"borderless"}
            extra={
              <Text type="secondary" style={{ fontSize: 12 }}>
                전체 {p.total ?? 0}개
              </Text>
            }
            style={{ height: '100%' }}
            styles={{
              header: {
                minHeight: 52,
              },
            }}
          >
            <ProjectStatusList stats={p} />
          </Card>
        </Col>

      </Row>
    </Flex>
  );
}