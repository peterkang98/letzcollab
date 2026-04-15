import { Form, Input, message, Modal } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';

export default function CreateWorkspaceModal({ open, onClose, onSuccess }) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (values) => api.post('/workspaces', {
      name: values.name,
      position: values.position || null,
    }),
    onSuccess: async () => {
      message.success('워크스페이스가 생성되었습니다.');
      await queryClient.invalidateQueries({ queryKey: ['workspaces'] });
      const res = await api.get('/workspaces');
      const list = res.data.data;
      const newWs = list[list.length - 1];
      form.resetFields();
      onSuccess(newWs.publicId);
    },
    onError: (e) => message.error(e.response?.data?.message || '생성에 실패했습니다.'),
  });

  const handleOk = async () => {
    const values = await form.validateFields();
    mutation.mutate(values);
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      open={open}
      title="새 워크스페이스 만들기"
      onCancel={handleCancel}
      onOk={handleOk}
      okText="만들기"
      cancelText="취소"
      confirmLoading={mutation.isPending}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="name"
          label="워크스페이스 이름"
          rules={[
            { required: true, message: '이름을 입력하세요.' },
            { min: 2, max: 30, message: '2자 이상 30자 이하로 입력해주세요.' },
          ]}
        >
          <Input placeholder="예: 개발팀, 마케팅팀" maxLength={30} showCount />
        </Form.Item>
        <Form.Item
          name="position"
          label="내 직책 (선택)"
          rules={[{ max: 100, message: '직책은 100자 이내로 입력해주세요.' }]}
        >
          <Input placeholder="예: 팀장, 개발자" maxLength={100} />
        </Form.Item>
      </Form>
    </Modal>
  );
}