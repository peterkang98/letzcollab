import { Menu, Skeleton, Typography } from 'antd';
import { DashboardOutlined, FolderOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';

const { Text } = Typography;

const NAV_ITEMS = [
  { key: '/', icon: <DashboardOutlined />, label: '대시보드' },
  { key: '/me', icon: <UserOutlined />, label: '내 정보' },
];
const DISABLED_KEYS = new Set(['proj-loading', 'proj-empty']);

export default function SidebarNav({ currentKey, selectedWorkspaceId, projects, isLoading, onNavigate }) {
  const projectMenuItems = isLoading
    ? [{ key: 'proj-loading', label: <Skeleton.Input active size="small" block/>, disabled: true }]
    : projects.length === 0
      ? [{
        key: 'proj-empty',
        label: <Text style={{ color: 'rgba(255,255,255,0.3)', fontSize: 12 }}>프로젝트가 없습니다</Text>,
        disabled: true
      }]
      : projects.map(p => ({
        key: `/workspaces/${selectedWorkspaceId}/projects/${p.publicId}`,
        icon: <FolderOutlined/>,
        label: p.name,
      }));

  const workspaceSettingsKey = selectedWorkspaceId ? `/workspaces/${selectedWorkspaceId}/settings` : null;

  const menuItems = [
    ...NAV_ITEMS,
    ...(workspaceSettingsKey
      ? [{
        key: workspaceSettingsKey,
        icon: <SettingOutlined />,
        label: '워크스페이스 설정',
      }]
      : []),
    { type: 'divider' },
    { key: 'projects-group', label: '프로젝트', type: 'group' },
    ...projectMenuItems,
  ];

  const handleClick = ({ key }) => {
    if (!DISABLED_KEYS.has(key)) onNavigate(key);
  };

  return (
    <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingTop: 8 }}>
      <Menu
        mode="inline"
        theme="dark"
        selectedKeys={[currentKey]}
        onClick={handleClick}
        items={menuItems}
        style={{ background: 'transparent', border: 'none' }}
      />
    </div>
  );
}