import { useState } from 'react';
import {
  Avatar, Badge, Button, Card, Descriptions, Flex,
  Form, Input, message, Modal, Skeleton, Space, Table, Tag, Typography,
} from 'antd';
import {
  CrownOutlined, DeleteOutlined, EditOutlined,
  ExclamationCircleOutlined, HomeOutlined, MailOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/axios.js';
import { useWorkspace } from '../contexts/WorkspaceContext.jsx';
import { useCurrentUser } from '../hooks/useCurrentUser.js';
import {
  WORKSPACE_ROLE_CONFIG,
  canManage,
  canInvite,
  getAssignableRoles,
} from '../constants/workspaceRole.js';
import InviteMemberModal from '../components/workspace/InviteMemberModal.jsx';
import EditMemberModal from '../components/workspace/EditMemberModal.jsx';
import { formatDateTime } from '../utils/dateUtils.js';

const { Title, Text } = Typography;
const { confirm } = Modal;

export default function WorkspaceSettingsPage() {
  const { workspacePublicId } = useParams();
  const nav = useNavigate();
  const queryClient = useQueryClient();
  const { switchWorkspace, workspaces } = useWorkspace();
  const { data: me } = useCurrentUser();

  const { data: ws, isLoading } = useQuery({
    queryKey: ['workspaceDetails', workspacePublicId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspacePublicId}`);
      return res.data.data;
    },
    enabled: !!workspacePublicId,
    refetchInterval: 1000 * 60,
    refetchIntervalInBackground: false,
  });

  const [editNameOpen, setEditNameOpen] = useState(false);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [editMember, setEditMember] = useState(null);

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['workspaceDetails', workspacePublicId] });

  const myMemberInfo = ws?.members?.find((m) => m.publicId === me?.publicId);
  const myRole = myMemberInfo?.role;
  const isOwner = ws?.isOwner ?? false;

  // 워크스페이스 이름 수정
  const updateNameMutation = useMutation({
    mutationFn: (newName) =>
      api.patch(`/workspaces/${workspacePublicId}`, { newName }),
    onSuccess: () => {
      message.success('워크스페이스 이름이 수정되었습니다.');
      setEditNameOpen(false);
      invalidate();
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  // 워크스페이스 삭제
  const deleteMutation = useMutation({
    mutationFn: () => api.delete(`/workspaces/${workspacePublicId}`),
    onSuccess: () => {
      message.success('워크스페이스가 삭제되었습니다.');
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
      const remaining = workspaces.filter((w) => w.publicId !== workspacePublicId);
      if (remaining.length > 0) switchWorkspace(remaining[0].publicId);
      nav('/');
    },
    onError: (e) => message.error(e.response?.data?.message || '삭제에 실패했습니다.'),
  });

  // 멤버 강퇴
  const kickMutation = useMutation({
    mutationFn: (memberPublicId) =>
      api.delete(`/workspaces/${workspacePublicId}/members/${memberPublicId}`),
    onSuccess: () => {
      message.success('멤버를 강퇴했습니다.');
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || '강퇴에 실패했습니다.'),
  });

  const handleDeleteWorkspace = () =>
    confirm({
      title: '워크스페이스를 삭제하시겠습니까?',
      icon: <ExclamationCircleOutlined />,
      content: '워크스페이스 내 모든 프로젝트와 데이터가 삭제됩니다. 이 작업은 되돌릴 수 없습니다.',
      okText: '삭제',
      okType: 'danger',
      cancelText: '취소',
      onOk: () => deleteMutation.mutate(),
    });

  const handleKick = (member) =>
    confirm({
      title: `"${member.name}"을(를) 강퇴하시겠습니까?`,
      icon: <ExclamationCircleOutlined />,
      okText: '강퇴',
      okType: 'danger',
      cancelText: '취소',
      onOk: () => kickMutation.mutate(member.publicId),
    });

  const columns = [
    {
      title: '멤버',
      dataIndex: 'name',
      render: (name, record) => (
        <Flex align="center" gap={10}>
          <Avatar size={32} icon={<UserOutlined />} />
          <Flex vertical gap={2}>
            <Flex align="center" gap={6}>
              <Text strong style={{ fontSize: 13 }}>{name}</Text>
              {record.publicId === ws?.owner?.publicId && (
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
      width: 120,
      render: (role) => {
        const cfg = WORKSPACE_ROLE_CONFIG[role] ?? { label: role, color: 'default' };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: '직책',
      dataIndex: 'position',
      width: 140,
      render: (pos) => <Text type="secondary" style={{ fontSize: 13 }}>{pos || '-'}</Text>,
    },
    {
      title: '참여일',
      dataIndex: 'createdAt',
      width: 140,
      render: (v) => <Text type="secondary" style={{ fontSize: 12 }}>{formatDateTime(v)}</Text>,
    },
    {
      title: '관리',
      key: 'action',
      width: 100,
      render: (_, record) => {
        const isSelf = record.publicId === me?.publicId;
        const manageable = !isSelf && canManage(myRole, record.role);
        if (!manageable) return null;
        return (
          <Space size={4}>
            <Button
              type="text" size="small" icon={<EditOutlined />}
              onClick={() => setEditMember(record)}
            />
            <Button
              type="text" size="small" danger icon={<DeleteOutlined />}
              loading={kickMutation.isPending}
              onClick={() => handleKick(record)}
            />
          </Space>
        );
      },
    },
  ];

  if (isLoading) {
    return (
      <div style={{ padding: '28px 32px', maxWidth: 900, margin: '0 auto' }}>
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    );
  }

  if (!ws) return null;

  return (
    <div style={{ padding: '28px 32px', maxWidth: 900, margin: '0 auto' }}>

      <Flex justify="space-between" align="flex-start" style={{ marginBottom: 24 }}>
        <Flex vertical gap={4}>
          <Title level={4} style={{ margin: 0 }}>워크스페이스 설정</Title>
          <Text type="secondary" style={{ fontSize: 13 }}>{ws.workspaceName}</Text>
        </Flex>
        {isOwner && (
          <Button
            danger icon={<DeleteOutlined />}
            onClick={handleDeleteWorkspace}
            loading={deleteMutation.isPending}
          >
            워크스페이스 삭제
          </Button>
        )}
      </Flex>

      <Card
        title={<Flex align="center" gap={8}><HomeOutlined /><span>워크스페이스 정보</span></Flex>}
        extra={
          isOwner && (
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => setEditNameOpen(true)}>
              이름 수정
            </Button>
          )
        }
        style={{ marginBottom: 24 }}
      >
        <Descriptions column={2} size="small">
          <Descriptions.Item label="워크스페이스 이름">
            <Text strong>{ws.workspaceName}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="소유자">
            <Flex align="center" gap={6}>
              <CrownOutlined style={{ color: '#faad14' }} />
              <Text>{ws.owner?.name}</Text>
            </Flex>
          </Descriptions.Item>
          <Descriptions.Item label="내 직책">
            <Text>{ws.myPosition || '-'}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="멤버 수">
            <Badge count={ws.memberCount} showZero color="#1677ff" />
          </Descriptions.Item>
          <Descriptions.Item label="생성일" span={2}>
            <Text type="secondary">{formatDateTime(ws.createdAt)}</Text>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card
        title={
          <Flex align="center" gap={8}>
            <TeamOutlined />
            <span>멤버 목록</span>
            <Badge count={ws.memberCount} showZero color="#8c8c8c" />
          </Flex>
        }
        extra={
          canInvite(myRole) && (
            <Button type="primary" size="small" icon={<MailOutlined />} onClick={() => setInviteOpen(true)}>
              멤버 초대
            </Button>
          )
        }
      >
        <Table
          rowKey="publicId"
          dataSource={ws.members}
          columns={columns}
          pagination={false}
          size="small"
        />
      </Card>

      <EditWorkspaceNameModal
        open={editNameOpen}
        currentName={ws.workspaceName}
        onClose={() => setEditNameOpen(false)}
        onSubmit={(newName) => updateNameMutation.mutate(newName)}
        isLoading={updateNameMutation.isPending}
      />

      <InviteMemberModal
        open={inviteOpen}
        workspacePublicId={workspacePublicId}
        onClose={() => setInviteOpen(false)}
        onSuccess={() => { setInviteOpen(false); invalidate(); }}
      />

      {editMember && (
        <EditMemberModal
          open={!!editMember}
          member={editMember}
          workspacePublicId={workspacePublicId}
          onClose={() => setEditMember(null)}
          onSuccess={() => { setEditMember(null); invalidate(); }}
          assignableRoles={getAssignableRoles(myRole)}
        />
      )}
    </div>
  );
}

function EditWorkspaceNameModal({ open, currentName, onClose, onSubmit, isLoading }) {
  const [form] = Form.useForm();

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit(values.newName);
  };

  return (
    <Modal
      open={open}
      title="워크스페이스 이름 수정"
      onCancel={onClose}
      onOk={handleOk}
      okText="저장"
      cancelText="취소"
      confirmLoading={isLoading}
      afterOpenChange={(v) => { if (v) form.setFieldsValue({ newName: currentName }); }}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="newName"
          label="새 이름"
          rules={[
            { required: true, message: '이름을 입력하세요.' },
            { min: 2, max: 50, message: '2자 이상 50자 이하로 입력해주세요.' },
          ]}
        >
          <Input placeholder="워크스페이스 이름" maxLength={50} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
}