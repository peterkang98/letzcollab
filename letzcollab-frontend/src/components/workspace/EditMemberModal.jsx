import { useEffect } from 'react';
import { Form, Input, message, Modal, Select } from 'antd';
import { useMutation } from '@tanstack/react-query';
import api from '../../api/axios.js';

/**
 * PATCH /workspaces/{workspacePublicId}/members/{memberPublicId}
 * Body: { position, role }
 * assignableRoles: [{ value, label }] — 내 역할보다 낮은 역할만
 */
export default function EditMemberModal({ open, member, workspacePublicId, onClose, onSuccess, assignableRoles, }) {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open && member) {
      form.setFieldsValue({
        position: member.position || '',
        role: member.role,
      });
    }
  }, [open, member, form]);

  const mutation = useMutation({
    mutationFn: (values) =>
      api.patch(`/workspaces/${workspacePublicId}/members/${member.publicId}`, {
        position: values.position || null,
        role: values.role,
      }),
    onSuccess: () => {
      message.success('멤버 정보가 수정되었습니다.');
      onSuccess?.();
    },
    onError: (e) => message.error(e.response?.data?.message || '수정에 실패했습니다.'),
  });

  const handleOk = async () => {
    const values = await form.validateFields();
    mutation.mutate(values);
  };

  return (
    <Modal
      open={open}
      title={`"${member?.name}" 멤버 정보 수정`}
      onCancel={onClose}
      onOk={handleOk}
      okText="저장"
      cancelText="취소"
      confirmLoading={mutation.isPending}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="role"
          label="역할"
          rules={[{ required: true, message: '역할을 선택하세요.' }]}
        >
          <Select
            options={assignableRoles}
            placeholder="역할 선택"
          />
        </Form.Item>
        <Form.Item
          name="position"
          label="직책 (선택)"
          rules={[{ max: 100, message: '직책은 100자 이내로 입력해주세요.' }]}
        >
          <Input placeholder="예: 백엔드 개발자" maxLength={100}/>
        </Form.Item>
      </Form>
    </Modal>
  );
}