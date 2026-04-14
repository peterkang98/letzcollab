import { Button, DatePicker, Flex, Form, Input, Modal, Select, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PRIORITY_CONFIG } from '../../constants/taskStatus.js';

const PRIORITY_OPTIONS = Object.entries(PRIORITY_CONFIG).map(([k, v]) => ({
  value: k,
  label: v.label,
}));

/**
 * 담당자 선택 옵션 필터링
 *
 * - ADMIN : VIEWER를 제외한 전체 멤버 선택 가능
 * - MEMBER: 본인(MEMBER) + 다른 MEMBER 권한 멤버만 선택 가능
 */
function buildAssigneeOptions(members, myRole, myPublicId) {
  if (!members) return [];

  return members
    .filter((m) => {
      if (myRole === 'ADMIN') return m.role !== 'VIEWER';
      if (myRole === 'MEMBER') return m.role === 'MEMBER'; // 본인 포함
      return false;
    })
    .map((m) => ({
      value: m.publicId,
      label: m.publicId === myPublicId ? `${m.name} (나)` : m.name,
    }));
}

export default function CreateSubTaskModal({
                                             open,
                                             onClose,
                                             parentTaskPublicId,
                                             projectPublicId,
                                             workspacePublicId,
                                             myRole,
                                             myPublicId,
                                           }) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  // 프로젝트 멤버 목록 (담당자 선택용)
  // ProjectDetailsResponse.members 에 role(enum), name, publicId 포함
  const { data: projectDetail } = useQuery({
    queryKey: ['project', workspacePublicId, projectPublicId],
    queryFn: async () => {
      const res = await api.get(
        `/workspaces/${workspacePublicId}/projects/${projectPublicId}`
      );
      return res.data.data;
    },
    staleTime: 0,
    enabled: open, // 모달 열릴 때만 fetch
  });

  const assigneeOptions = buildAssigneeOptions(
    projectDetail?.members,
    myRole,
    myPublicId
  );

  const createMutation = useMutation({
    mutationFn: (values) =>
      api.post(`/projects/${projectPublicId}/tasks/${parentTaskPublicId}/subtasks`, {
        name: values.name,
        description: values.description ?? null,
        assigneePublicId: values.assigneePublicId,
        priority: values.priority,
        dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD') : null,
      }),
    onSuccess: () => {
      message.success('하위 업무가 생성되었습니다.');
      queryClient.invalidateQueries({ queryKey: ['task', parentTaskPublicId] });
      form.resetFields();
      onClose();
    },
    onError: (e) => message.error(e.response?.data?.message || '생성에 실패했습니다.'),
  });

  return (
    <Modal
      title="하위 업무 추가"
      open={open}
      onCancel={() => { form.resetFields(); onClose(); }}
      footer={null}
      width={480}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => createMutation.mutate(values)}
        style={{ marginTop: 16 }}
      >
        <Form.Item
          name="name"
          label="업무 이름"
          rules={[{ required: true, min: 2, max: 255, message: '2자 이상 255자 이하로 입력해주세요.' }]}
        >
          <Input placeholder="업무 이름을 입력하세요" />
        </Form.Item>

        <Form.Item name="description" label="설명">
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 5 }} placeholder="설명 (선택)" />
        </Form.Item>

        <Form.Item
          name="assigneePublicId"
          label="담당자"
          rules={[{ required: true, message: '담당자를 선택해주세요.' }]}
        >
          <Select
            placeholder="담당자 선택"
            options={assigneeOptions}
            notFoundContent="선택 가능한 멤버가 없습니다"
          />
        </Form.Item>

        <Form.Item
          name="priority"
          label="우선순위"
          rules={[{ required: true, message: '우선순위를 선택해주세요.' }]}
        >
          <Select placeholder="우선순위 선택" options={PRIORITY_OPTIONS} />
        </Form.Item>

        <Form.Item name="dueDate" label="마감일">
          <DatePicker style={{ width: '100%' }} placeholder="마감일 선택 (선택)" />
        </Form.Item>

        <Flex justify="flex-end" gap={8}>
          <Button onClick={() => { form.resetFields(); onClose(); }}>취소</Button>
          <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
            생성
          </Button>
        </Flex>
      </Form>
    </Modal>
  );
}