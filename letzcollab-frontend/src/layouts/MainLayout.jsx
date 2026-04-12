import { Badge, Button, Drawer, Flex, Grid, Layout, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { BellOutlined, FolderOpenFilled, MenuOutlined } from '@ant-design/icons';
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import api from '../api/axios.js';
import NotificationDrawer from "../components/sidebar/NotificationDrawer.jsx";
import SidebarContent from "../components/sidebar/SidebarContent.jsx";

const { Sider, Header, Content } = Layout;
const { Text } = Typography;
const { useBreakpoint } = Grid

const SIDEBAR_WIDTH = 220;
const SIDEBAR_BG = '#1d1d1d';

export default function MainLayout() {
  const user = JSON.parse(localStorage.getItem('user'));
  const nav = useNavigate();
  const location = useLocation();
  const isDesktop = useBreakpoint().lg;

  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isNotifOpen, setIsNotifOpen] = useState(false);

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: async () => {
      const res = await api.get('/notifications/unread-count');
      return res.data.data ?? 0;
    },
    staleTime: 0,
    refetchInterval: 1000 * 30,         // 30초마다 polling
    refetchIntervalInBackground: false, // 탭 비활성 시 polling 중단
  });

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

  const NAV_KEYS = ['/', '/me'];
  const currentKey = NAV_KEYS.find(key => location.pathname === key) ?? '/';

  // 메뉴 클릭 시 페이지 이동 + 모바일 드로어 닫기
  const handleNavClose = (key) => {
    nav(key);
    setIsMenuOpen(false);
  };

  const sidebarProps = {
    user,
    currentKey,
    unreadCount,
    onNotifOpen: () => setIsNotifOpen(true),
    onLogout: handleLogout,
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>

      {/* 데스크탑: 고정 사이드바 */}
      {isDesktop && (
        <Sider
          width={SIDEBAR_WIDTH}
          style={{
            background: SIDEBAR_BG,
            position: 'fixed',
            left: 0, top: 0, bottom: 0,
            zIndex: 100,
            overflow: 'hidden',
          }}
        >
          <SidebarContent {...sidebarProps} onClose={handleNavClose} />
        </Sider>
      )}

      {/* 모바일: 상단 헤더 */}
      {!isDesktop && (
        <Header style={{
          background: SIDEBAR_BG,
          padding: '0 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          position: 'sticky',
          top: 0,
          zIndex: 100,
          height: 56,
          lineHeight: '56px',
        }}>
          <Flex align="center" gap={8}>
            <FolderOpenFilled style={{ fontSize: 20, color: '#fff' }} />
            <Text strong style={{ fontSize: 16, color: '#fff', letterSpacing: '-0.5px' }}>
              Let'z Collab
            </Text>
          </Flex>
          <Flex gap={4} align="center">
            <Badge count={unreadCount} size="small" offset={[-2, 2]}>
              <Button
                type="text"
                icon={<BellOutlined />}
                onClick={() => setIsNotifOpen(true)}
                style={{ color: 'rgba(255,255,255,0.8)' }}
              />
            </Badge>
            <Button
              type="text"
              icon={<MenuOutlined />}
              onClick={() => setIsMenuOpen(true)}
              style={{ color: 'rgba(255,255,255,0.8)' }}
            />
          </Flex>
        </Header>
      )}

      {/* 모바일 메뉴 드로어 */}
      <Drawer
        open={isMenuOpen}
        onClose={() => setIsMenuOpen(false)}
        placement="left"
        styles={{
          wrapper: { width: SIDEBAR_WIDTH },
          body: { padding: 0, background: SIDEBAR_BG },
          header: { display: 'none' },
        }}
      >
        <SidebarContent {...sidebarProps} onClose={handleNavClose} />
      </Drawer>

      {/* 알림 드로어 */}
      <NotificationDrawer
        open={isNotifOpen}
        onClose={() => setIsNotifOpen(false)}
      />

      {/* 콘텐츠 영역 */}
      <Layout style={{
        marginLeft: isDesktop ? SIDEBAR_WIDTH : 0,
        background: '#e0e7f5',
        minHeight: '100vh',
      }}>
        <Content>
          <Outlet />
        </Content>
      </Layout>

    </Layout>
  );
};