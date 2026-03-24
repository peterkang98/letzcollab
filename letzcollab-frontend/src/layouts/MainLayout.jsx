import { Layout, Flex, Typography, Avatar, Dropdown } from 'antd';
import { Outlet } from 'react-router-dom';
import { LogoutOutlined, UserOutlined, FolderOpenFilled } from '@ant-design/icons';
import api from '../api/axios.js';

const { Header, Content } = Layout;
const { Text } = Typography;

export default function MainLayout() {
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

  const menuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '로그아웃',
      onClick: handleLogout,
      danger: true,
    }
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ background: '#fff', borderBottom: '1px solid #f0f0f0', padding: '0 40px' }}>
        <Flex justify="space-between" align="center" style={{ height: '100%' }}>
          <Flex align="center" gap={8}>
            <FolderOpenFilled style={{ fontSize: 22, color: '#1d1d1d' }} />
            <Text strong style={{ fontSize: 18, letterSpacing: '-0.5px' }}>Let'z Collab</Text>
          </Flex>
          <Dropdown menu={{ items: menuItems }} placement="bottomRight">
            <Flex align="center" gap={8} style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} size="small" />
              <Text style={{ fontSize: 14 }}>{user?.name}</Text>
            </Flex>
          </Dropdown>
        </Flex>
      </Header>
      <Content>
        <Outlet />
      </Content>
    </Layout>
  );
};