import { Form, Input, message, Modal, Select } from 'antd';
import { useMutation } from '@tanstack/react-query';
import api from '../../api/axios.js';
import { PROJECT_ROLE_OPTIONS } from '../../constants/projectRole.js';

export default function EditProjectMemberModal({ open, member, isLeader, workspacePublicId, projectPublicId, onClose, onSuccess, }) {
  const [form] = Form.useForm();

  const mutation = useMutation({
    mutationFn: (values) =>
      api.patch(`/workspaces/${workspacePublicId}/projects/${projectPublicId}/members/${member.publicId}`, {
        targetUserPublicId: member.publicId,
        newPosition: values.newPosition || null,
        newRole: values.newRole,
      }),
    onSuccess: () => {
      message.success('멤버 정보가 수정되었습니다.');
      onSuccess();
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  const roleOptions = isLeader ? PROJECT_ROLE_OPTIONS : PROJECT_ROLE_OPTIONS.filter(r => r.value !== 'ADMIN');

  return (
    <Modal
      open={open}
      title={`"${member?.name}" 멤버 수정`}
      onCancel={onClose}
      onOk={() => form.validateFields().then(v => mutation.mutate(v))}
      okText="저장"
      cancelText="취소"
      confirmLoading={mutation.isPending}
      afterOpenChange={(v) => {
        if (v) form.setFieldsValue({ newRole: member.role, newPosition: member.position || '' });
      }}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="newRole" label="역할"
                   rules={[{ required: true, message: '역할을 선택하세요.' }]}>
          <Select options={roleOptions} />
        </Form.Item>
        <Form.Item name="newPosition" label="직책 (선택)"
                   rules={[{ min: 2, max: 100, message: '2자 이상 100자 이하로 입력해주세요.' }]}>
          <Input placeholder="예: 프론트엔드 개발자" maxLength={100} />
        </Form.Item>
      </Form>
    </Modal>
  );
}