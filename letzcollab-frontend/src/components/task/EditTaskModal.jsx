import { Button, Flex, Form, Input, message, Modal, Select } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PRIORITY_CONFIG, STATUS_CONFIG } from '../../constants/taskStatus.js';

const { TextArea } = Input;

// ASSIGNEE는 DONE/CANCELLED 불가
const ASSIGNEE_ALLOWED_STATUSES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW'];

const ALL_STATUS_OPTIONS = Object.entries(STATUS_CONFIG).map(([k, v]) => ({ value: k, label: v.label }));
const ASSIGNEE_STATUS_OPTIONS = ASSIGNEE_ALLOWED_STATUSES.map((k) => ({ value: k, label: STATUS_CONFIG[k].label }));
const PRIORITY_OPTIONS = Object.entries(PRIORITY_CONFIG).map(([k, v]) => ({ value: k, label: v.label }));

export default function EditTaskModal({ open, onClose, task, projectPublicId, canEditAll }) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const updateMutation = useMutation({
    mutationFn: (values) =>
      api.patch(`/projects/${projectPublicId}/tasks/${task.publicId}`, {
        ...values,
        dueDate: values.dueDate || null,
      }),
    onSuccess: () => {
      message.success('업무가 수정되었습니다.');
      queryClient.invalidateQueries({ queryKey: ['task', task.publicId] });
      onClose();
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  return (
    <Modal
      title="업무 수정"
      open={open}
      onCancel={onClose}
      footer={null}
      width={520}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          name: task?.name,
          description: task?.description,
          status: task?.status,
          priority: task?.priority,
          dueDate: task?.dueDate,
        }}
        onFinish={(values) => updateMutation.mutate(values)}
        style={{ marginTop: 16 }}
      >
        {canEditAll && (
          <>
            <Form.Item name="name" label="업무 이름" rules={[{ required: true, min: 2, max: 255 }]}>
              <Input/>
            </Form.Item>
            <Form.Item name="description" label="설명">
              <TextArea autoSize={{ minRows: 3, maxRows: 8 }}/>
            </Form.Item>
          </>
        )}

        <Form.Item name="status" label="상태">
          <Select options={canEditAll ? ALL_STATUS_OPTIONS : ASSIGNEE_STATUS_OPTIONS}/>
        </Form.Item>

        {canEditAll && (
          <>
            <Form.Item name="priority" label="우선순위">
              <Select options={PRIORITY_OPTIONS}/>
            </Form.Item>
            <Form.Item name="dueDate" label="마감일">
              <Input type="date"/>
            </Form.Item>
          </>
        )}

        <Flex justify="flex-end" gap={8}>
          <Button onClick={onClose}>취소</Button>
          <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
            저장
          </Button>
        </Flex>
      </Form>
    </Modal>
  );
}