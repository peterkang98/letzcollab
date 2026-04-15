import { useState } from 'react';
import { Avatar, Badge, Button, Card, Descriptions, Flex, message, Modal, Skeleton, Space, Table, Tag, Typography, } from 'antd';
import { CrownOutlined, DeleteOutlined, EditOutlined, ExclamationCircleOutlined, LockOutlined, PlusOutlined, TeamOutlined, UnlockOutlined, UserOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/axios.js';
import { useWorkspace } from '../contexts/WorkspaceContext.jsx';
import { useMyProjectRole } from '../hooks/useMyProjectRole.js';
import { useCurrentUser } from '../hooks/useCurrentUser.js';
import KanbanBoard from '../components/project/KanbanBoard.jsx';
import CreateTaskModal from '../components/project/CreateTaskModal.jsx';
import EditProjectModal from '../components/project/EditProjectModal.jsx';
import AddProjectMemberModal from '../components/project/AddProjectMemberModal.jsx';
import EditProjectMemberModal from '../components/project/EditProjectMemberModal.jsx';
import { PROJECT_STATUS_CONFIG } from '../constants/projectStatus.js';
import { PROJECT_ROLE_CONFIG } from '../constants/projectRole.js';
import { formatDateTime } from '../utils/dateUtils.js';

const { Title, Text } = Typography;
const { confirm } = Modal;

export default function ProjectDetailPage() {
  const { workspacePublicId, projectPublicId } = useParams();
  const nav = useNavigate();
  const queryClient = useQueryClient();
  const { data: me } = useCurrentUser();
  const { selectedWorkspaceId } = useWorkspace();
  const { data: myMember } = useMyProjectRole(projectPublicId);

  const [editOpen, setEditOpen] = useState(false);
  const [createTaskOpen, setCreateTaskOpen] = useState(false);
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [editMemberTarget, setEditMemberTarget] = useState(null);

  // 프로젝트 상세 조회
  const { data: project, isLoading: projLoading } = useQuery({
    queryKey: ['project', workspacePublicId, projectPublicId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspacePublicId}/projects/${projectPublicId}`);
      return res.data.data;
    },
    refetchInterval: 1000 * 60,
    refetchIntervalInBackground: false,
  });

  // 업무 목록 조회
  const { data: tasksData, isLoading: tasksLoading } = useQuery({
    queryKey: ['tasks', projectPublicId],
    queryFn: async () => {
      const res = await api.get(`/projects/${projectPublicId}/tasks`, {
        params: { size: 200, sort: 'createdAt,desc' },
      });
      return res.data.data.content;
    },
    refetchInterval: 1000 * 60,
    refetchIntervalInBackground: false,
  });

  const tasks = tasksData ?? [];

  const invalidateProject = () =>
    queryClient.invalidateQueries({ queryKey: ['project', workspacePublicId, projectPublicId] });

  // 권한 판별
  const myRole = myMember?.role;
  const isLeader = project?.leader?.publicId === me?.publicId;
  const isAdmin = myRole === 'ADMIN';
  const isMember = myRole === 'MEMBER';
  const canEdit = isAdmin;
  const canDelete = isLeader;
  const canManageMembers = isAdmin;
  const canCreateTask = isAdmin || isMember;

  // 프로젝트 삭제
  const deleteMutation = useMutation({
    mutationFn: () => api.delete(`/workspaces/${workspacePublicId}/projects/${projectPublicId}`),
    onSuccess: () => {
      message.success('프로젝트가 삭제되었습니다.');
      queryClient.invalidateQueries({ queryKey: ['sidebarProjects', selectedWorkspaceId] });
      nav('/');
    },
    onError: (e) => message.error(e.response?.data?.message || '삭제에 실패했습니다.'),
  });

  // 멤버 강퇴
  const kickMutation = useMutation({
    mutationFn: (targetUserPublicId) =>
      api.delete(`/workspaces/${workspacePublicId}/projects/${projectPublicId}/members/${targetUserPublicId}`),
    onSuccess: () => {
      message.success('멤버를 강퇴했습니다.');
      invalidateProject();
    },
    onError: (e) => message.error(e.response?.data?.message || '강퇴에 실패했습니다.'),
  });

  const handleDelete = () =>
    confirm({
      title: '프로젝트를 삭제하시겠습니까?',
      icon: <ExclamationCircleOutlined />,
      content: '모든 업무와 댓글이 함께 삭제됩니다. 이 작업은 되돌릴 수 없습니다.',
      okText: '삭제', okType: 'danger', cancelText: '취소',
      onOk: () => deleteMutation.mutate(),
    });

  const handleKick = (member) =>
    confirm({
      title: `"${member.name}"을(를) 강퇴하시겠습니까?`,
      icon: <ExclamationCircleOutlined />,
      okText: '강퇴', okType: 'danger', cancelText: '취소',
      onOk: () => kickMutation.mutate(member.publicId),
    });

  // 멤버 테이블 컬럼
  const memberColumns = [
    {
      title: '멤버',
      dataIndex: 'name',
      render: (name, record) => (
        <Flex align="center" gap={8}>
          <Avatar size={28} icon={<UserOutlined />} />
          <Flex vertical gap={2}>
            <Flex align="center" gap={6}>
              <Text strong style={{ fontSize: 13 }}>{name}</Text>
              {record.publicId === project?.leader?.publicId && (
                <CrownOutlined style={{ fontSize: 12, color: '#faad14' }} />
              )}
              {record.publicId === me?.publicId && (
                <Tag style={{ margin: 0, fontSize: 10 }}>나</Tag>
              )}
            </Flex>
            <Text type="secondary" style={{ fontSize: 11 }}>{record.email}</Text>
          </Flex>
        </Flex>
      ),
    },
    {
      title: '역할',
      dataIndex: 'role',
      width: 100,
      render: (role) => {
        const cfg = PROJECT_ROLE_CONFIG[role] ?? { label: role, color: 'default' };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: '직책',
      dataIndex: 'position',
      width: 130,
      render: (pos) => <Text type="secondary" style={{ fontSize: 13 }}>{pos || '-'}</Text>,
    },
    {
      title: '참여일',
      dataIndex: 'createdAt',
      width: 130,
      render: (v) => <Text type="secondary" style={{ fontSize: 12 }}>{formatDateTime(v)}</Text>,
    },
    {
      title: '관리',
      key: 'action',
      width: 90,
      render: (_, record) => {
        const isSelf = record.publicId === me?.publicId;
        const isTargetLeader = record.publicId === project?.leader?.publicId;
        if (!canManageMembers || isSelf || isTargetLeader) return null;
        return (
          <Space size={4}>
            <Button type="text" size="small" icon={<EditOutlined />}
                    onClick={() => setEditMemberTarget(record)} />
            <Button type="text" size="small" danger icon={<DeleteOutlined />}
                    onClick={() => handleKick(record)} />
          </Space>
        );
      },
    },
  ];

  if (projLoading) {
    return (
      <div style={{ padding: '28px 32px' }}>
        <Skeleton active paragraph={{ rows: 6 }} />
      </div>
    );
  }

  if (!project) return null;

  const status = PROJECT_STATUS_CONFIG[project.status] ?? { label: project.status, color: 'default' };

  return (
    <div style={{ padding: '28px 32px', maxWidth: 1400, margin: '0 auto' }}>

      {/* 헤더 */}
      <Flex justify="space-between" align="flex-start" style={{ marginBottom: 24 }} wrap="wrap" gap={12}>
        <Flex vertical gap={6}>
          <Flex align="center" gap={10}>
            {project.isPrivate
              ? <LockOutlined style={{ color: '#8c8c8c' }} />
              : <UnlockOutlined style={{ color: '#8c8c8c' }} />
            }
            <Title level={4} style={{ margin: 0 }}>{project.projectName}</Title>
            <Tag color={status.color}>{status.label}</Tag>
          </Flex>
          {project.description && (
            <Text type="secondary" style={{ fontSize: 13 }}>{project.description}</Text>
          )}
        </Flex>
        <Flex gap={8}>
          {canEdit && (
            <Button icon={<EditOutlined />} onClick={() => setEditOpen(true)}>수정</Button>
          )}
          {canDelete && (
            <Button danger icon={<DeleteOutlined />} onClick={handleDelete} loading={deleteMutation.isPending}>
              삭제
            </Button>
          )}
        </Flex>
      </Flex>

      {/* 프로젝트 정보 */}
      <Card title="프로젝트 정보" size="small" style={{ marginBottom: 16 }}>
        <Descriptions column={{ xs: 1, sm: 2, md: 4 }} size="small">
          <Descriptions.Item label="리더">
            <Flex align="center" gap={6}>
              <CrownOutlined style={{ color: '#faad14', fontSize: 12 }} />
              <Text>{project.leader?.name}</Text>
            </Flex>
          </Descriptions.Item>
          <Descriptions.Item label="기간">
            <Text style={{ fontSize: 12 }}>{project.startDate ?? '?'} ~ {project.endDate ?? '?'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="공개 여부">
            {project.isPrivate
              ? <Tag icon={<LockOutlined />}>비공개</Tag>
              : <Tag icon={<UnlockOutlined />}>공개</Tag>
            }
          </Descriptions.Item>
          <Descriptions.Item label="생성일">
            <Text type="secondary" style={{ fontSize: 12 }}>{formatDateTime(project.createdAt)}</Text>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 업무 현황 (칸반보드) */}
      <Card
        title="업무 현황"
        size="small"
        loading={tasksLoading}
        style={{ marginBottom: 16 }}
        extra={
          canCreateTask && (
            <Button type="primary" size="small" icon={<PlusOutlined />}
                    onClick={() => setCreateTaskOpen(true)}>
              업무 추가
            </Button>
          )
        }
      >
        <KanbanBoard
          tasks={tasks}
          projectPublicId={projectPublicId}
        />
      </Card>

      {/* 멤버 */}
      <Card
        title={
          <Flex align="center" gap={8}>
            <TeamOutlined />
            <span>멤버</span>
            <Badge count={project.memberCount} showZero color="#8c8c8c" />
          </Flex>
        }
        extra={
          canManageMembers && (
            <Button type="text" size="small" icon={<PlusOutlined />}
                    onClick={() => setAddMemberOpen(true)}>
              추가
            </Button>
          )
        }
        size="small"
      >
        <Table
          rowKey="publicId"
          dataSource={project.members}
          columns={memberColumns}
          pagination={false}
          size="small"
          scroll={{ x: true }}
        />
      </Card>

      {createTaskOpen && (
        <CreateTaskModal
          open={createTaskOpen}
          projectPublicId={projectPublicId}
          members={project.members ?? []}
          onClose={() => setCreateTaskOpen(false)}
          onSuccess={() => setCreateTaskOpen(false)}
        />
      )}

      {editOpen && (
        <EditProjectModal
          open={editOpen}
          project={project}
          isLeader={isLeader}
          workspacePublicId={workspacePublicId}
          projectPublicId={projectPublicId}
          onClose={() => setEditOpen(false)}
          onSuccess={() => { setEditOpen(false); invalidateProject(); }}
        />
      )}

      {addMemberOpen && (
        <AddProjectMemberModal
          open={addMemberOpen}
          workspacePublicId={workspacePublicId}
          projectPublicId={projectPublicId}
          isLeader={isLeader}
          onClose={() => setAddMemberOpen(false)}
          onSuccess={() => { setAddMemberOpen(false); invalidateProject(); }}
        />
      )}

      {editMemberTarget && (
        <EditProjectMemberModal
          open={!!editMemberTarget}
          member={editMemberTarget}
          isLeader={isLeader}
          workspacePublicId={workspacePublicId}
          projectPublicId={projectPublicId}
          onClose={() => setEditMemberTarget(null)}
          onSuccess={() => { setEditMemberTarget(null); invalidateProject(); }}
        />
      )}
    </div>
  );
}