import { Form, Input, message, Modal, Select } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PROJECT_ROLE_OPTIONS } from '../../constants/projectRole.js';

export default function AddProjectMemberModal({ open, workspacePublicId, projectPublicId, isLeader, onClose, onSuccess, }) {
  const [form] = Form.useForm();

  const { data: wsDetails } = useQuery({
    queryKey: ['workspaceDetails', workspacePublicId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${workspacePublicId}`);
      return res.data.data;
    },
  });

  const wsMembers = wsDetails?.members ?? [];

  const mutation = useMutation({
    mutationFn: (values) =>
      api.post(`/workspaces/${workspacePublicId}/projects/${projectPublicId}/members`, {
        targetUserPublicId: values.targetUserPublicId,
        role: values.role,
        position: values.position || null,
      }),
    onSuccess: () => {
      message.success('멤버가 추가되었습니다.');
      form.resetFields();
      onSuccess();
    },
    onError: (e) => message.error(e.response?.data?.message || '추가에 실패했습니다.'),
  });

  const roleOptions = isLeader
    ? PROJECT_ROLE_OPTIONS
    : PROJECT_ROLE_OPTIONS.filter(r => r.value !== 'ADMIN');

  return (
    <Modal
      open={open}
      title="멤버 추가"
      onCancel={() => { form.resetFields(); onClose(); }}
      onOk={() => form.validateFields().then(v => mutation.mutate(v))}
      okText="추가"
      cancelText="취소"
      confirmLoading={mutation.isPending}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="targetUserPublicId" label="워크스페이스 멤버"
                   rules={[{ required: true, message: '멤버를 선택하세요.' }]}>
          <Select
            placeholder="멤버 선택"
            showSearch={{ optionFilterProp: 'label' }}
            options={wsMembers.map(m => ({ value: m.publicId, label: `${m.name} (${m.email})` }))}
          />
        </Form.Item>
        <Form.Item name="role" label="역할"
                   rules={[{ required: true, message: '역할을 선택하세요.' }]}>
          <Select options={roleOptions} />
        </Form.Item>
        <Form.Item name="position" label="직책 (선택)"
                   rules={[{ min: 2, max: 100, message: '2자 이상 100자 이하로 입력해주세요.' }]}>
          <Input placeholder="예: 백엔드 개발자" maxLength={100} />
        </Form.Item>
      </Form>
    </Modal>
  );
}