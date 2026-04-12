import { Avatar, Dropdown, Flex, Skeleton, Typography } from 'antd';
import { CheckOutlined, SwapOutlined } from '@ant-design/icons';

const { Text } = Typography;

export default function WorkspaceSwitcher({ user, workspaces, selectedWorkspace, isLoading, onSwitch }) {
  const items = [
    {
      key: 'ws-header',
      label: (
        <Flex vertical gap={2} style={{ padding: '4px 0' }}>
          <Text strong style={{ fontSize: 13 }}>{selectedWorkspace?.name ?? '워크스페이스'}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>{user?.email}</Text>
        </Flex>
      ),
      disabled: true,
    },
    { type: 'divider' },
    ...workspaces.map(ws => ({
      key: ws.publicId,
      icon: ws.publicId === selectedWorkspace?.publicId
        ? <CheckOutlined style={{ color: '#1677ff' }} />
        : <span style={{ width: 14, display: 'inline-block' }} />,
      label: (
        <Flex justify="space-between" align="center" gap={8}>
          <Text style={{ fontSize: 13 }}>{ws.name}</Text>
          {ws.myPosition && (
            <Text type="secondary" style={{ fontSize: 11 }}>{ws.myPosition}</Text>
          )}
        </Flex>
      ),
    }))
  ];

  const handleClick = ({ key }) => {
    if (key !== 'ws-header') onSwitch(key);
  };

  return (
    <Dropdown menu={{ items, onClick: handleClick }} trigger={['click']} placement="bottomLeft">
      <Flex
        align="center"
        gap={10}
        style={{
          padding: '16px 16px 14px',
          cursor: 'pointer',
          borderBottom: '1px solid rgba(255,255,255,0.08)',
          transition: 'background 0.2s',
          flexShrink: 0,
        }}
        onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.06)'}
        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
      >
        <Avatar
          size={28}
          style={{ background: '#1677ff', fontSize: 13, fontWeight: 700, flexShrink: 0 }}
        >
          {selectedWorkspace?.name?.[0] ?? 'W'}
        </Avatar>

        {isLoading ? (
          <Skeleton.Input active size="small" style={{ flex: 1 }} />
        ) : (
          <Text style={{
            color: '#fff',
            fontSize: 14,
            fontWeight: 600,
            flex: 1,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}>
            {selectedWorkspace?.name ?? '워크스페이스 선택'}
          </Text>
        )}
        <SwapOutlined style={{ color: 'rgba(255,255,255,0.45)', fontSize: 12, flexShrink: 0 }} />
      </Flex>
    </Dropdown>
  );
}