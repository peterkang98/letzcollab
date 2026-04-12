import { Avatar, Badge, Button, Dropdown, Flex, Typography } from 'antd';
import { BellOutlined, CaretUpOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons';

const { Text } = Typography;

export default function SidebarFooter({ user, unreadCount, onNotifOpen, onLogout, onNavigate }) {
  const dropdownItems = [
    {
      key: 'header',
      label: (
        <Flex vertical gap={2} style={{ padding: '4px 0', maxWidth: 180 }}>
          <Text strong style={{ fontSize: 13 }}>{user?.name}</Text>
          <Text type="secondary" style={{ fontSize: 11, wordBreak: 'break-all' }}>
            {user?.email}
          </Text>
        </Flex>
      ),
      disabled: true,
    },
    { type: 'divider' },
    { key: '/me', icon: <UserOutlined />, label: '내 정보' },
    { key: 'logout', icon: <LogoutOutlined />, label: '로그아웃', danger: true },
  ];

  const handleDropdownClick = ({ key }) => {
    if (key === 'logout') onLogout();
    else if (key === '/me') onNavigate('/me');
  };

  return (
    <div style={{ padding: '0 16px 16px', flexShrink: 0 }}>
      <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: 14 }}>
        <Flex align="center" justify="space-between" gap={8}>

          {/* 유저 Dropdown */}
          <Dropdown
            menu={{ items: dropdownItems, onClick: handleDropdownClick }}
            placement="topLeft"
            trigger={['click']}
          >
            <Flex
              align="center"
              gap={6}
              style={{
                cursor: 'pointer',
                overflow: 'hidden',
                flex: 1,
                padding: '4px 6px',
                borderRadius: 6,
                transition: 'background 0.2s',
              }}
              onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.08)'}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
            >
              <Avatar
                icon={<UserOutlined />}
                size={28}
                style={{ background: 'rgba(255,255,255,0.15)', flexShrink: 0 }}
              />
              <Text style={{
                color: '#fff',
                fontSize: 13,
                fontWeight: 600,
                flex: 1,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}>
                {user?.name}
              </Text>
              <CaretUpOutlined style={{ color: 'rgba(255,255,255,0.45)', fontSize: 10, flexShrink: 0 }} />
            </Flex>
          </Dropdown>

          {/* 알림 벨 */}
          <Badge count={unreadCount} size="small" offset={[-2, 2]}>
            <Button
              type="text"
              icon={<BellOutlined />}
              onClick={onNotifOpen}
              style={{ color: 'rgba(255,255,255,0.65)', flexShrink: 0 }}
            />
          </Badge>

        </Flex>
      </div>
    </div>
  );
}