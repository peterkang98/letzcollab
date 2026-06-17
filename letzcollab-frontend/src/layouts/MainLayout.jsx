import { Badge, Button, Drawer, Flex, Grid, Layout, Typography, notification } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { BellOutlined, FolderOpenFilled, MenuOutlined } from '@ant-design/icons';
import { useEffect, useState } from "react";
import api from '../api/axios.js';
import NotificationDrawer from "../components/sidebar/NotificationDrawer.jsx";
import SidebarContent from "../components/sidebar/SidebarContent.jsx";
import { WorkspaceProvider } from "../contexts/WorkspaceContext.jsx";
import { useNotificationSSE } from "../hooks/useNotificationSSE.js";
import { NOTIFICATION_TYPE_CONFIG, buildNotifLink } from '../constants/notificationConfig.js';


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
  const { unreadCount, latestNotification, clearLatestNotification, disconnect } = useNotificationSSE();

  const handleLogout = async () => {
    disconnect();

    try {
      await api.post('/auth/logout');
    } catch (e) {
      console.error("로그아웃 API 호출 실패", e);
    } finally {
      localStorage.clear();
      window.location.href = '/login';
    }
  }

  const currentKey = location.pathname;

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

  useEffect(() => {
    if (!latestNotification) return;

    const typeInfo = NOTIFICATION_TYPE_CONFIG[latestNotification.type] ?? {};
    const link = buildNotifLink(latestNotification);

    notification.open({
      title: latestNotification.message,
      description: typeInfo.label,
      icon: <BellOutlined style={{ color: '#1677ff' }} />,
      placement: 'topRight',
      duration: 10,
      actions: link ? (
        <Button
          type="primary"
          size="small"
          onClick={() => {
            notification.destroy();
            nav(link);
          }}
        >
          바로가기
        </Button>
      ) : undefined,
    });
    // 팝업 띄운 후 다음 알림을 받을 수 있도록 초기화
    clearLatestNotification();
  }, [latestNotification, clearLatestNotification, nav]);

  return (
    <WorkspaceProvider>
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
            <SidebarContent {...sidebarProps}/>
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
              <FolderOpenFilled style={{ fontSize: 20, color: '#fff' }}/>
              <Text strong style={{ fontSize: 16, color: '#fff', letterSpacing: '-0.5px' }}>
                Let'z Collab
              </Text>
            </Flex>
            <Flex gap={4} align="center">
              <Badge count={unreadCount} size="small" offset={[-2, 2]}>
                <Button
                  type="text"
                  icon={<BellOutlined/>}
                  onClick={() => setIsNotifOpen(true)}
                  style={{ color: 'rgba(255,255,255,0.8)' }}
                />
              </Badge>
              <Button
                type="text"
                icon={<MenuOutlined/>}
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
          <SidebarContent {...sidebarProps} onClose={handleNavClose}/>
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
            <Outlet/>
          </Content>
        </Layout>

      </Layout>
    </WorkspaceProvider>
  );
};