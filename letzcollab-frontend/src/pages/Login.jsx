import { Button, Card, Divider, Flex, Form, message, Typography } from 'antd';
import { Link, useNavigate } from "react-router-dom";
import { LockOutlined, MailOutlined } from "@ant-design/icons";
import AuthFormInput from "../components/AuthFormInput.jsx";
import { useMutation } from "@tanstack/react-query";
import api from "../api/axios.js";

const { Title, Text } = Typography;

export default function Login() {
  const nav = useNavigate();

  const loginMutation = useMutation({
    mutationFn: async (loginData) => {
      const res = await api.post("/auth/login", loginData, {
        headers: {
          "X-Client-Type": "web"
        }
      });
      return res.data;
    },
    onSuccess: (res) => {
      const { name, email } = res.data;

      localStorage.setItem('user', JSON.stringify({
        name, email
      }));
      message.success('로그인에 성공했습니다!');

      nav("/", { replace: true });
    },
    onError: (error) => {
      const res = error.response?.data;
      message.error(res.message);
    }
  });

  return (
    <Card style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px' }}>
      <section>
        <Flex vertical align="center" style={{ marginBottom: 20 }}>
          <Title level={3} style={{ margin: 5 }}>프로젝트 협업 관리 툴</Title>
          <Text type="secondary">계정에 로그인하세요</Text>
        </Flex>
        <Form layout="vertical" requiredMark={false} onFinish={(data) => loginMutation.mutate(data)}>
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
          <AuthFormInput
            name="password"
            label="비밀번호"
            icon={LockOutlined}
            isPassword={true}
            rules={[{ required: true, message: "비밀번호를 입력해주세요!" }]}
            placeholder="••••••••"
          />
          <Flex justify="end" style={{ marginBottom: 15 }}>
            <Link to="/auth/password/reset-request">비밀번호 재설정</Link>
          </Flex>
          <Button color="default" variant="solid" htmlType="submit" block loading={loginMutation.isPending}>
            로그인
          </Button>
        </Form>
      </section>
      <Divider>
        <Text type="secondary">또는</Text>
      </Divider>
      <footer>
        <Flex justify="center">
          <Text>
            계정이 없으신가요? <Link to="/auth/signup">회원가입</Link>
          </Text>
        </Flex>
      </footer>
    </Card>
  );
}
