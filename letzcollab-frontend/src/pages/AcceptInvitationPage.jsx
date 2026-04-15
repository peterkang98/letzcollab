import { useEffect } from 'react';
import { Button, Card, Flex, Result, Spin, Typography } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../api/axios.js';
import { INVITATION_TOKEN_KEY } from '../constants/storageKeys.js';

const { Text } = Typography;

/**
 * 진입 시나리오:
 *  1. 로그인 O → 바로 수락 API 호출
 *  2. 로그인 X → token을 localStorage에 저장 → 로그인 페이지로 이동
 *               → 로그인/회원가입 완료 후 PrivateRoute를 통과하면 이 페이지로 다시 돌아와 수락 처리
 */
export default function AcceptInvitationPage() {
  const nav = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const isLoggedIn = !!localStorage.getItem('user');

  const { mutate: acceptInvitation, isPending, isSuccess, isError, error } = useMutation({
    mutationFn: () =>
      api.post('/workspaces/invitations/accept', { token }),
    onSuccess: () => {
      localStorage.removeItem(INVITATION_TOKEN_KEY);
    },
    onError: () => {
      localStorage.removeItem(INVITATION_TOKEN_KEY);
    },
  });

  useEffect(() => {
    if (!token) return;

    if (!isLoggedIn) {
      localStorage.setItem(INVITATION_TOKEN_KEY, token);
      nav('/auth/login', { replace: true });
      return;
    }

    // 로그인된 상태면 바로 수락
    acceptInvitation();
  }, [token, isLoggedIn, acceptInvitation, nav]);

  if (!token) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
        <Card style={{ maxWidth: 400, width: '100%' }}>
          <Result
            status="error"
            title="유효하지 않은 초대 링크"
            subTitle="초대 링크가 올바르지 않습니다."
            extra={<Button type="primary" onClick={() => nav('/')}>홈으로</Button>}
          />
        </Card>
      </Flex>
    );
  }

  if (isPending) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
        <Flex vertical align="center" gap={16}>
          <Spin size="large" />
          <Text type="secondary">초대를 수락하는 중...</Text>
        </Flex>
      </Flex>
    );
  }

  if (isSuccess) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
        <Card style={{ maxWidth: 400, width: '100%' }}>
          <Result
            status="success"
            title="워크스페이스 참여 완료!"
            subTitle="이제부터 워크스페이스 멤버로 활동할 수 있습니다."
            extra={<Button type="primary" onClick={() => nav('/')}>워크스페이스로 이동</Button>}
          />
        </Card>
      </Flex>
    );
  }

  if (isError) {
    const msg = error?.response?.data?.message || '초대 수락에 실패했습니다. 링크가 만료되었거나 이미 사용된 링크일 수 있습니다.';
    return (
      <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
        <Card style={{ maxWidth: 400, width: '100%' }}>
          <Result
            status="error"
            title="초대 수락 실패"
            subTitle={msg}
            extra={<Button type="primary" onClick={() => nav('/')}>홈으로</Button>}
          />
        </Card>
      </Flex>
    );
  }

  return null;
}