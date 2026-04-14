import { Flex, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios.js';
import WorkspaceSwitcher from './WorkspaceSwitcher.jsx';
import SidebarNav from './SidebarNav.jsx';
import SidebarFooter from './SidebarFooter.jsx';
import { useWorkspace } from "../../contexts/WorkspaceContext.jsx";
import { FolderOpenFilled } from "@ant-design/icons";

const { Text } = Typography;

export default function SidebarContent({ user, currentKey, unreadCount, onNotifOpen, onLogout, onClose }) {
  const nav = useNavigate();
  const { workspaces, wsLoading, selectedWorkspace, selectedWorkspaceId, switchWorkspace } = useWorkspace()

  // 선택된 워크스페이스의 프로젝트 목록
  const { data: projects = [], isLoading: projLoading } = useQuery({
    queryKey: ['sidebarProjects', selectedWorkspaceId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${selectedWorkspaceId}/projects`);
      return res.data.data.content;
    },
    enabled: !!selectedWorkspaceId,
  });

  const handleNavigate = (key) => {
    nav(key);
    onClose?.();
  };

  return (
    <Flex vertical style={{ height: '100%', overflow: 'hidden' }}>
      <Flex
        align="center"
        gap={8}
        style={{ padding: '18px 16px 14px', flexShrink: 0 }}
      >
        <FolderOpenFilled style={{ fontSize: 20, color: '#fff' }} />
        <Text strong style={{ fontSize: 15, color: '#fff', letterSpacing: '-0.5px' }}>
          Let'z Collab
        </Text>
      </Flex>
      <WorkspaceSwitcher
        user={user}
        workspaces={workspaces}
        selectedWorkspace={selectedWorkspace}
        isLoading={wsLoading}
        onSwitch={switchWorkspace}
      />
      <SidebarNav
        currentKey={currentKey}
        selectedWorkspaceId={selectedWorkspaceId}
        projects={projects}
        isLoading={projLoading}
        onNavigate={handleNavigate}
      />
      <SidebarFooter
        user={user}
        unreadCount={unreadCount}
        onNotifOpen={() => { onNotifOpen(); onClose?.(); }}
        onLogout={onLogout}
        onNavigate={handleNavigate}
      />
    </Flex>
  );
}