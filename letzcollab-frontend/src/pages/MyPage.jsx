import { useState } from 'react';
import {
  Avatar, Button, Card, Descriptions, Flex,
  Form, Input, message, Modal, Skeleton, Typography,
} from 'antd';
import {
  DeleteOutlined, EditOutlined, ExclamationCircleOutlined, UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import { useCurrentUser } from '../hooks/useCurrentUser.js';

const { Title, Text } = Typography;
const { confirm } = Modal;

export default function MyPage() {
  const nav = useNavigate();
  const queryClient = useQueryClient();
  const { data: me, isLoading } = useCurrentUser();
  const [editOpen, setEditOpen] = useState(false);

  // ── 내 정보 수정 ──────────────────────────────────────────────────────────
  const updateMutation = useMutation({
    mutationFn: (values) => api.patch('/users/me', values),
    onSuccess: (res) => {
      message.success('내 정보가 수정되었습니다.');
      // localStorage의 user도 name 업데이트
      const updated = res.data.data;
      const stored = JSON.parse(localStorage.getItem('user') || '{}');
      localStorage.setItem('user', JSON.stringify({ ...stored, name: updated.name }));
      queryClient.invalidateQueries({ queryKey: ['me'] });
      setEditOpen(false);
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  // ── 회원 탈퇴 ─────────────────────────────────────────────────────────────
  const withdrawMutation = useMutation({
    mutationFn: () => api.delete('/users/me'),
    onSuccess: () => {
      message.success('탈퇴가 완료되었습니다.');
      localStorage.clear();
      window.location.href = '/auth/login';
    },
    onError: (e) => message.error(e.response?.data?.message || '탈퇴에 실패했습니다.'),
  });

  const handleWithdraw = () =>
    confirm({
      title: '정말 탈퇴하시겠습니까?',
      icon: <ExclamationCircleOutlined />,
      content: '탈퇴 후에는 계정을 복구할 수 없습니다.',
      okText: '탈퇴',
      okType: 'danger',
      cancelText: '취소',
      onOk: () => withdrawMutation.mutate(),
    });

  if (isLoading) {
    return (
      <div style={{ padding: '28px 32px', maxWidth: 600, margin: '0 auto' }}>
        <Skeleton active avatar paragraph={{ rows: 4 }} />
      </div>
    );
  }

  return (
    <div style={{ padding: '28px 32px', maxWidth: 600, margin: '0 auto' }}>

      <Title level={4} style={{ margin: '0 0 24px' }}>내 정보</Title>

      <Card>
        {/* 아바타 + 이름 */}
        <Flex align="center" gap={16} style={{ marginBottom: 24 }}>
          <Avatar size={64} icon={<UserOutlined />} style={{ background: '#1677ff', flexShrink: 0 }} />
          <Flex vertical gap={4}>
            <Text strong style={{ fontSize: 18 }}>{me?.name}</Text>
            <Text type="secondary" style={{ fontSize: 13 }}>{me?.email}</Text>
          </Flex>
        </Flex>

        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="이름">{me?.name}</Descriptions.Item>
          <Descriptions.Item label="이메일">{me?.email}</Descriptions.Item>
          <Descriptions.Item label="전화번호">{me?.phoneNumber || '-'}</Descriptions.Item>
        </Descriptions>

        <Flex justify="space-between" style={{ marginTop: 20 }}>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={handleWithdraw}
            loading={withdrawMutation.isPending}
          >
            회원 탈퇴
          </Button>
          <Button
            type="primary"
            icon={<EditOutlined />}
            onClick={() => setEditOpen(true)}
          >
            정보 수정
          </Button>
        </Flex>
      </Card>

      {/* 정보 수정 모달 */}
      <EditProfileModal
        open={editOpen}
        me={me}
        onClose={() => setEditOpen(false)}
        onSubmit={(values) => updateMutation.mutate(values)}
        isLoading={updateMutation.isPending}
      />
    </div>
  );
}

function EditProfileModal({ open, me, onClose, onSubmit, isLoading }) {
  const [form] = Form.useForm();

  const handleOk = async () => {
    const values = await form.validateFields();
    // 빈 문자열은 null로 변환 (전화번호 삭제 케이스)
    onSubmit({
      name: values.name,
      phoneNumber: values.phoneNumber || null,
    });
  };

  return (
    <Modal
      open={open}
      title="내 정보 수정"
      onCancel={onClose}
      onOk={handleOk}
      okText="저장"
      cancelText="취소"
      confirmLoading={isLoading}
      afterOpenChange={(v) => {
        if (v) form.setFieldsValue({ name: me?.name, phoneNumber: me?.phoneNumber || '' });
      }}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="name"
          label="이름"
          rules={[
            { required: true, message: '이름을 입력하세요.' },
            { min: 2, max: 100, message: '2자 이상 100자 이하로 입력해주세요.' },
          ]}
        >
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item label="이메일">
          <Input value={me?.email} disabled />
        </Form.Item>
        <Form.Item
          name="phoneNumber"
          label="전화번호 (선택)"
          rules={[{
            pattern: /^(\d{2,3}-\d{3,4}-\d{4})?$/,
            message: '올바른 전화번호 형식이어야 합니다. (예: 010-1234-5678)',
          }]}
        >
          <Input placeholder="010-1234-5678" maxLength={20} />
        </Form.Item>
      </Form>
    </Modal>
  );
}