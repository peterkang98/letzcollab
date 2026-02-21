import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import api from "../api/axios.js";
import { Button, Card, Flex, Form, message, Modal, Typography } from "antd";
import AuthFormInput from "../components/AuthFormInput.jsx";
import { LockOutlined, MailOutlined, PhoneOutlined, UserOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

export default function Signup() {
  const nav = useNavigate();

  const signupMutation = useMutation({
    mutationFn: async (signupData) => {
      const res = await api.post("/auth/signup", signupData);
      return res.data;
    },
    onSuccess: (res) => {
      Modal.success({
        title: res.message,
        content: (
          <div>
            <p>입력하신 이메일 수신함을 확인해주세요.</p>
          </div>
        ),
        onOk: () => nav("/auth/login", {replace: true})
      });
    },
    onError: (error) => {
      const serverRes = error.response?.data;

      if (serverRes?.errorCode === "U002") {
        message.error("이미 사용 중인 이메일입니다. 다른 이메일을 입력해주세요.");
      } else {
        message.error("회원가입에 실패했습니다. 재시도 부탁드립니다.");
      }
    }
  });

  const onFinish = (data) => {
    const {confirmPassword, ...signupData} = data;

    if (!signupData.phoneNumber) {
      signupData.phoneNumber = null;
    }

    signupMutation.mutate(signupData);
  };

  const validate = ({ getFieldValue }) => {
    return {
      validator(_, value) {
        if (!value || getFieldValue('password') === value) {
          return Promise.resolve();
        }
        return Promise.reject(new Error('비밀번호가 일치하지 않습니다!'));
      }
    }
  };

  return (
    <Card style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px' }}>
      <section>
        <Flex vertical align="center" style={{ marginBottom: 20 }}>
          <Title level={3} style={{ margin: 5 }}>회원가입</Title>
          <Text type="secondary">새로운 계정을 만들어 시작하세요</Text>
        </Flex>
        <Form layout="vertical" requiredMark={false} onFinish={onFinish}>
          <AuthFormInput
            name="name"
            label="이름"
            icon={UserOutlined}
            rules={[{ required: true, message: "이름을 입력해주세요!" }]}
            placeholder="홍길동"
          />
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
            rules={[
              { required: true, message: "비밀번호를 입력해주세요!" },
              {
                pattern: "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                message: "영문자, 숫자, 특수문자를 조합하여 최소 8자 이상 입력해주세요!"
              }
            ]}
            placeholder="영문자, 숫자, 특수문자를 조합하여 최소 8자 이상"
          />
          <AuthFormInput
            name="confirmPassword"
            label="비밀번호 확인"
            icon={LockOutlined}
            isPassword={true}
            dependencies={["password"]}
            hasFeedback
            rules={[
              { required: true, message: "비밀번호를 한 번 더 입력해주세요!" },
              validate
            ]}
            placeholder="비밀번호 재입력"
          />
          <AuthFormInput
            name="phoneNumber"
            label="전화번호 (선택)"
            icon={PhoneOutlined}
            rules={[
              { required: false},
              {
                pattern: "^(\\d{2,3}-\\d{3,4}-\\d{4})?$",
                message: "하이픈을 넣어서 입력해주세요!"
              }
            ]}
            placeholder="010-1234-5678"
          />
          <Button color="default" variant="solid" type="default" htmlType="submit" block
                  loading={signupMutation.isPending} style={{ marginTop: 15 }}
          >
            회원가입
          </Button>
        </Form>
      </section>
      <footer>
        <Flex justify="center" style={{ marginTop: 15 }}>
          <Text>
            이미 계정이 있으신가요? <Link to="/auth/login">로그인</Link>
          </Text>
        </Flex>
      </footer>
    </Card>
  );
};