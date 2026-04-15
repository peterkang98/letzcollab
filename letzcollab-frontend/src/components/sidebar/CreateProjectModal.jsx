import { DatePicker, Form, Input, message, Modal, Select, Switch } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PROJECT_STATUS_CONFIG } from '../../constants/projectStatus.js';

const STATUS_OPTIONS = Object.entries(PROJECT_STATUS_CONFIG).map(([key, cfg]) => ({
  value: key,
  label: cfg.label,
}));


export default function CreateProjectModal({ open, workspacePublicId, onClose, onSuccess }) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (values) => api.post(`/workspaces/${workspacePublicId}/projects`, {
      name: values.name,
      description: values.description || null,
      status: values.status,
      startDate: values.startDate?.format('YYYY-MM-DD') ?? null,
      endDate: values.endDate?.format('YYYY-MM-DD') ?? null,
      isPrivate: values.isPrivate ?? false,
      position: values.position || null,
    }),
    onSuccess: () => {
      message.success('프로젝트가 생성되었습니다.');
      queryClient.invalidateQueries({ queryKey: ['sidebarProjects', workspacePublicId] });
      form.resetFields();
      onSuccess?.();
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
      title="새 프로젝트 만들기"
      onCancel={handleCancel}
      onOk={handleOk}
      okText="만들기"
      cancelText="취소"
      confirmLoading={mutation.isPending}
    >
      <Form
        form={form}
        layout="vertical"
        style={{ marginTop: 16 }}
        initialValues={{ status: 'PLANNED', isPrivate: false }}
      >
        <Form.Item
          name="name"
          label="프로젝트 이름"
          rules={[
            { required: true, message: '이름을 입력하세요.' },
            { min: 2, max: 100, message: '2자 이상 100자 이하로 입력해주세요.' },
          ]}
        >
          <Input placeholder="예: 백엔드 API 개발" maxLength={100} showCount />
        </Form.Item>

        <Form.Item
          name="description"
          label="설명 (선택)"
        >
          <Input.TextArea
            placeholder="프로젝트에 대한 설명을 입력하세요."
            autoSize={{ minRows: 2, maxRows: 4 }}
            maxLength={10000}
          />
        </Form.Item>

        <Form.Item
          name="status"
          label="상태"
          rules={[{ required: true, message: '상태를 선택하세요.' }]}
        >
          <Select options={STATUS_OPTIONS} />
        </Form.Item>

        <Form.Item label="기간 (선택)" style={{ marginBottom: 0 }}>
          <Form.Item name="startDate" style={{ display: 'inline-block', width: 'calc(50% - 6px)' }}>
            <DatePicker placeholder="시작일" style={{ width: '100%' }} />
          </Form.Item>
          <span style={{ display: 'inline-block', width: 12, textAlign: 'center' }}>~</span>
          <Form.Item name="endDate" style={{ display: 'inline-block', width: 'calc(50% - 6px)' }}>
            <DatePicker placeholder="종료일" style={{ width: '100%' }} />
          </Form.Item>
        </Form.Item>

        <Form.Item name="isPrivate" label="비공개" valuePropName="checked">
          <Switch />
        </Form.Item>

        <Form.Item
          name="position"
          label="내 직책 (선택)"
          rules={[{ min: 2, max: 100, message: '2자 이상 100자 이하로 입력해주세요.' }]}
        >
          <Input placeholder="예: 백엔드 개발자" maxLength={100} />
        </Form.Item>
      </Form>
    </Modal>
  );
}