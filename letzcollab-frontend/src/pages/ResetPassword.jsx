import { Form, Input, Button, Card, Typography, message, Result } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { useMutation } from "@tanstack/react-query";
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from "../api/axios.js";
import AuthFormInput from "../components/AuthFormInput.jsx";

const { Title} = Typography;

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const nav = useNavigate();
  const token = searchParams.get('token');

  const { mutate, isPending, isSuccess, data } = useMutation({
    mutationFn: async ({ password }) => {
      const res = await api.post('/auth/password/reset', { token, newPassword: password });
      return res.data;
    },
    onError: (error) => {
      message.error(error.response?.data?.message || '비밀번호 재설정에 실패했습니다.');
    }
  });

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

  if (isSuccess) {
    return (
      <section>
        <Card style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px' }}>
          <Result
            status="success"
            title="비밀번호 변경 완료"
            subTitle={data?.message || "새로운 비밀번호로 로그인해주세요."}
            extra={<Button type="primary" onClick={() => nav('/auth/login', {replace: true})}>로그인하러 가기</Button>}
          />
        </Card>
      </section>
    );
  }

  return (
      <Card style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px' }}>
        <section>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>새로운 비밀번호 설정</Title>

        <Form layout="vertical" requiredMark={false} onFinish={(values) => mutate(values)}>
          <AuthFormInput
            name="password"
            label="새로운 비밀번호"
            icon={LockOutlined}
            isPassword={true}
            rules={[
              { required: true, message: "새로운 비밀번호를 입력해주세요!" },
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

          <Button htmlType="submit" block size="large" loading={isPending} color="default" variant="solid">
            비밀번호 변경하기
          </Button>
        </Form>
        </section>
      </Card>
  );
}