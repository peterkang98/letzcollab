import { Layout } from 'antd';
import { Outlet, useNavigate } from 'react-router-dom';
import api from '../api/axios.js';

const { Header, Content } = Layout;

export default function MainLayout() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user'));

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (e) {
      console.error("로그아웃 API 호출 실패", e);
    } finally {
      localStorage.clear();
      window.location.href = '/login';
    }
  }

  return (
    <Layout>
      <Header>

      </Header>
      <Content>
        <Outlet/>
      </Content>
    </Layout>
  );
};