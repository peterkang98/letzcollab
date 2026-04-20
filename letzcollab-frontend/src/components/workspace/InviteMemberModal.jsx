import { Alert, Form, Input, message, Modal } from 'antd';
import { useMutation } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { useState } from "react";

export default function InviteMemberModal({ open, workspacePublicId, onClose, onSuccess }) {
  const [form] = Form.useForm();
  const [rateLimited, setRateLimited] = useState(false);

  const mutation = useMutation({
    mutationFn: (values) =>
      api.post(`/workspaces/${workspacePublicId}/invitations`, {
        email: values.email,
        position: values.position || null,
      }),
    onSuccess: () => {
      message.success('초대 이메일이 발송되었습니다. 피초청인이 24시간 이내에 수락해야 합니다.');
      form.resetFields();
      onSuccess?.();
    },
    onError: (e) => {
      const serverError = e.response?.data;
      if (serverError.errorCode === 'E005') {
        setRateLimited(true);
      } else {
        message.error(serverError?.message || '초대에 실패했습니다.');
      }
    }
  });

  const handleOk = async () => {
    const values = await form.validateFields();
    mutation.mutate(values);
  };

  const handleCancel = () => {
    form.resetFields();
    setRateLimited(false);
    onClose();
  };

  return (
    <Modal
      open={open}
      title="멤버 초대"
      onCancel={handleCancel}
      onOk={handleOk}
      okText="초대 이메일 발송"
      cancelText="취소"
      confirmLoading={mutation.isPending}
      okButtonProps={{ disabled: rateLimited }}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        {rateLimited && (
          <Alert
            type="warning"
            title="초대 요청 횟수를 초과했습니다. 1시간 후에 다시 시도해주세요."
            style={{ marginBottom: 16 }}
            showIcon
          />
        )}
        <Form.Item
          name="email"
          label="이메일"
          rules={[
            { required: true, message: '이메일을 입력하세요.' },
            { type: 'email', message: '올바른 이메일 형식이어야 합니다.' },
          ]}
        >
          <Input placeholder="초대할 멤버의 이메일" />
        </Form.Item>
        <Form.Item
          name="position"
          label="직책 (선택)"
          rules={[{ max: 100, message: '직책은 100자 이내로 입력해주세요.' }]}
        >
          <Input placeholder="예: 프론트엔드 개발자" maxLength={100} />
        </Form.Item>
      </Form>
    </Modal>
  );
}