import { DatePicker, Form, Input, message, Modal, Select, Switch } from 'antd';
import { useMutation } from '@tanstack/react-query';
import dayjs from 'dayjs';
import api from '../../api/axios.js';
import { PROJECT_STATUS_CONFIG } from '../../constants/projectStatus.js';

const STATUS_OPTIONS = Object.entries(PROJECT_STATUS_CONFIG).map(([k, v]) => ({ value: k, label: v.label }));

export default function EditProjectModal({ open, project, isLeader, workspacePublicId, projectPublicId, onClose, onSuccess, }) {
  const [form] = Form.useForm();

  const mutation = useMutation({
    mutationFn: (values) => api.patch(`/workspaces/${workspacePublicId}/projects/${projectPublicId}`, {
      newName: values.newName,
      newDescription: values.newDescription || null,
      newStatus: values.newStatus,
      newStartDate: values.newStartDate?.format('YYYY-MM-DD') ?? null,
      newEndDate: values.newEndDate?.format('YYYY-MM-DD') ?? null,
      newIsPrivate: isLeader ? values.newIsPrivate : undefined,
    }),
    onSuccess: () => {
      message.success('프로젝트가 수정되었습니다.');
      onSuccess();
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  return (
    <Modal
      open={open}
      title="프로젝트 수정"
      onCancel={onClose}
      onOk={() => form.validateFields().then(v => mutation.mutate(v))}
      okText="저장"
      cancelText="취소"
      confirmLoading={mutation.isPending}
      afterOpenChange={(v) => {
        if (v) form.setFieldsValue({
          newName: project.projectName,
          newDescription: project.description,
          newStatus: project.status,
          newStartDate: project.startDate ? dayjs(project.startDate) : null,
          newEndDate: project.endDate ? dayjs(project.endDate) : null,
          newIsPrivate: project.isPrivate,
        });
      }}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="newName" label="이름" rules={[{ required: true }, { min: 2, max: 100 }]}>
          <Input maxLength={100} showCount />
        </Form.Item>
        <Form.Item name="newDescription" label="설명 (선택)">
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} maxLength={10000} />
        </Form.Item>
        <Form.Item name="newStatus" label="상태">
          <Select options={STATUS_OPTIONS} />
        </Form.Item>
        <Form.Item label="기간" style={{ marginBottom: 0 }}>
          <Form.Item name="newStartDate" style={{ display: 'inline-block', width: 'calc(50% - 6px)' }}>
            <DatePicker placeholder="시작일" style={{ width: '100%' }} />
          </Form.Item>
          <span style={{ display: 'inline-block', width: 12, textAlign: 'center' }}>~</span>
          <Form.Item name="newEndDate" style={{ display: 'inline-block', width: 'calc(50% - 6px)' }}>
            <DatePicker placeholder="종료일" style={{ width: '100%' }} />
          </Form.Item>
        </Form.Item>
        {isLeader && (
          <Form.Item name="newIsPrivate" label="비공개" valuePropName="checked">
            <Switch />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
}