import { Button, Flex, Menu, Skeleton, Typography } from 'antd';
import { DashboardOutlined, FolderOutlined, PlusOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { canInvite } from "../../constants/workspaceRole.js";
import {useWorkspace} from "../../contexts/WorkspaceContext.jsx";
import { useState } from "react";
import CreateProjectModal from "./CreateProjectModal.jsx";

const { Text } = Typography;

const NAV_ITEMS = [
  { key: '/', icon: <DashboardOutlined />, label: '대시보드' },
  { key: '/me', icon: <UserOutlined />, label: '내 정보' },
];
const DISABLED_KEYS = new Set(['proj-loading', 'proj-empty']);

export default function SidebarNav({ currentKey, selectedWorkspaceId, projects, isLoading, onNavigate }) {

  const { myWorkspaceRole } = useWorkspace();
  const [createProjectOpen, setCreateProjectOpen] = useState(false);

  // OWNER / ADMIN만 프로젝트 생성 가능
  const canCreateProject = canInvite(myWorkspaceRole);

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
    {
      key: 'projects-group',
      label: (
        <Flex justify="space-between" align="center">
          <span>프로젝트</span>
          {canCreateProject && (
            <Button
              type="text"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => { e.stopPropagation(); setCreateProjectOpen(true); }}
              style={{ color: 'rgba(255,255,255,0.45)', padding: '0 4px', height: 'auto' }}
            />
          )}
        </Flex>
      ),
      type: 'group',
    },
    ...projectMenuItems,
  ];

  const handleClick = ({ key }) => {
    if (!DISABLED_KEYS.has(key)) onNavigate(key);
  };

  return (
    <>
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

      {selectedWorkspaceId && (
        <CreateProjectModal
          open={createProjectOpen}
          workspacePublicId={selectedWorkspaceId}
          onClose={() => setCreateProjectOpen(false)}
          onSuccess={() => setCreateProjectOpen(false)}
        />
      )}
    </>
  );
}