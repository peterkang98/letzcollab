import { DatePicker, Form, Input, message, Modal, Select } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PRIORITY_CONFIG } from '../../constants/taskStatus.js';

const PRIORITY_OPTIONS = Object.entries(PRIORITY_CONFIG).map(([k, v]) => ({ value: k, label: v.label }));


export default function CreateTaskModal({ open, projectPublicId, members, onClose, onSuccess, }) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  // VIEWER는 담당자로 지정 불가
  const assigneeOptions = members
    .filter(m => m.role !== 'VIEWER')
    .map(m => ({ value: m.publicId, label: `${m.name}${m.position ? ` (${m.position})` : ''}` }));

  const mutation = useMutation({
    mutationFn: (values) =>
      api.post(`/projects/${projectPublicId}/tasks`, {
        name: values.name,
        description: values.description || null,
        assigneePublicId: values.assigneePublicId,
        priority: values.priority,
        dueDate: values.dueDate?.format('YYYY-MM-DD') ?? null,
      }),
    onSuccess: () => {
      message.success('업무가 생성되었습니다.');
      queryClient.invalidateQueries({ queryKey: ['tasks', projectPublicId] });
      form.resetFields();
      onSuccess?.();
    },
    onError: (e) => message.error(e.response?.data?.message || '업무 생성에 실패했습니다.'),
  });

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      open={open}
      title="업무 추가"
      onCancel={handleCancel}
      onOk={() => form.validateFields().then(v => mutation.mutate(v))}
      okText="추가"
      cancelText="취소"
      confirmLoading={mutation.isPending}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}
            initialValues={{ priority: 'MEDIUM' }}>
        <Form.Item name="name" label="업무 이름"
                   rules={[{ required: true, message: '이름을 입력하세요.' }, { min: 2, max: 255 }]}>
          <Input placeholder="업무 이름을 입력하세요" maxLength={255} />
        </Form.Item>
        <Form.Item name="description" label="설명 (선택)">
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} maxLength={10000} />
        </Form.Item>
        <Form.Item name="assigneePublicId" label="담당자"
                   rules={[{ required: true, message: '담당자를 선택하세요.' }]}>
          <Select placeholder="담당자 선택" showSearch={{ optionFilterProp: 'label' }} options={assigneeOptions} />
        </Form.Item>
        <Form.Item name="priority" label="우선순위"
                   rules={[{ required: true, message: '우선순위를 선택하세요.' }]}>
          <Select options={PRIORITY_OPTIONS} />
        </Form.Item>
        <Form.Item name="dueDate" label="마감일 (선택)">
          <DatePicker style={{ width: '100%' }} placeholder="마감일 선택" />
        </Form.Item>
      </Form>
    </Modal>
  );
}