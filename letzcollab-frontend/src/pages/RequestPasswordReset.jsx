import { Form, Input, Button, Card, Typography, message, Flex } from "antd";
import { ArrowLeftOutlined, MailOutlined } from '@ant-design/icons';
import api from "../api/axios.js";
import { useMutation } from "@tanstack/react-query";
import AuthFormInput from "../components/AuthFormInput.jsx";
import { Link } from "react-router-dom";

const { Title, Text } = Typography;

export default function RequestPasswordReset() {
  const [form] = Form.useForm();

  const { mutate, isPending } = useMutation({
    mutationFn: async (values) => {
      const res = await api.post('/auth/password/reset-request', values);
      return res.data;
    },
    onSuccess: (data) => {
      message.success(data.message || '비밀번호 재설정 메일을 발송했습니다.');
      form.resetFields();
    },
    onError: (error) => {
      const serverError = error.response?.data;
      message.error(serverError?.message || '메일 발송 중 오류가 발생했습니다.');
    }
  });

  return (

      <Card style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px' }}>
        <section>
        <Flex vertical style={{ marginBottom: 20 }}>
          <Title level={3} style={{ marginBottom: 5 }}>비밀번호 재설정</Title>
          <Text type="secondary">가입하신 이메일 주소를 입력하시면 <br/>비밀번호 재설정 링크를 보내드립니다.</Text>
        </Flex>
        <Form form={form} requiredMark={false} layout="vertical" onFinish={(values) => mutate(values)}>
          <AuthFormInput
            name="email"
            label="이메일"
            icon={MailOutlined}
            rules={[
              { required: true, message: "이메일을 입력해주세요!" },
              { type: 'email', message: '올바른 이메일 형식이 아닙니다.' }
            ]}
            placeholder="example@gmail.com"
          />
          <Button htmlType="submit" block size="large" loading={isPending} color="default" variant="solid">
            재설정 링크 전송
          </Button>
        </Form>
        </section>
        <footer>
          <Flex justify="center" style={{ marginTop: 20 }}>
            <Link to="/auth/login" style={{ color: '#000000e0' }}>
              <ArrowLeftOutlined style={{ marginRight: 8, fontSize: '12px' }}/>
              <Text >로그인 페이지로 돌아가기</Text>
            </Link>
          </Flex>
        </footer>
      </Card>
  );
}