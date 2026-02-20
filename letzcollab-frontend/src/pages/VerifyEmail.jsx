import { Button, Card, message, Modal, Result, Spin } from "antd";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useEffect } from "react";
import api from "../api/axios.js";
import { useMutation } from "@tanstack/react-query";

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const nav = useNavigate();
  const token = searchParams.get('token');

  // 이메일 인증 요청
  const { mutate: verifyMutate, isPending, isError, error, data } = useMutation({
    mutationFn: async ({ token }) => {
      const res = await api.post('/auth/verify-email', { token });
      return res.data;
    }
  });

  // 이메일 인증 토큰 만료 시, 토큰 재발급 요청
  const { mutate: resendMutate, isPending: isResending } = useMutation({
    mutationFn: async ({ token }) => {
      const res = await api.post('/auth/verify-email/resend', { expiredToken: token });
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
    onError: (err) => {
      message.error(err.response?.data?.message || "재발송 요청에 실패했습니다.");
    }
  });

  useEffect(() => {
    if (token) {
      verifyMutate({ token });
    }
  }, [token, verifyMutate]);

  if (isPending || (!token && !isError)) {
    return (
      <div style={{ textAlign: 'center', marginTop: 100 }}>
        <Spin size="large"/>
        <div style={{ marginTop: 16 }}>이메일 확인 중...</div>
      </div>
    );
  }

  const isTokenExpired = error?.response?.data?.errorCode === "E003";

  return (

      <Card style={{ boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px' }}>
        <section>
        <Result
          status={isError ? "error" : "success"}
          title={isError ? "인증 실패" : "인증 완료!"}
          subTitle={isError ? (error.response?.data?.message) : data?.message}
          extra={[
            <Button color="default" variant="solid" key="login" onClick={() => nav("/auth/login")}>
              로그인하러 가기
            </Button>,
            isError && isTokenExpired && (
              <Button
                key="resend"
                loading={isResending}
                onClick={() => resendMutate({ token })}
              >
                인증 메일 재발송
              </Button>
            )
          ]}
        />
        </section>
      </Card>
  );
};