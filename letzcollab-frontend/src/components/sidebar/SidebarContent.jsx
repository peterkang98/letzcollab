import { useState } from 'react';
import { Flex } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios.js';
import WorkspaceSwitcher from './WorkspaceSwitcher.jsx';
import SidebarNav from './SidebarNav.jsx';
import SidebarFooter from './SidebarFooter.jsx';

const LAST_WORKSPACE_KEY = 'lastWorkspaceId';

export default function SidebarContent({ user, currentKey, unreadCount, onNotifOpen, onLogout, onClose }) {
  const nav = useNavigate();

  const [savedId, setSavedId] = useState(
    () => localStorage.getItem(LAST_WORKSPACE_KEY) ?? null
  );

  // 워크스페이스 목록
  const { data: workspaces = [], isLoading: wsLoading } = useQuery({
    queryKey: ['workspaces'],
    queryFn: async () => {
      const res = await api.get('/workspaces');
      return res.data.data;
    },
  });

  // savedId가 목록에 존재하면 사용, 없거나 null이면 첫 번째 워크스페이스
  const selectedWorkspace =
    workspaces.find(ws => ws.publicId === savedId) ?? workspaces[0] ?? null;
  const selectedWorkspaceId = selectedWorkspace?.publicId ?? null;

  // 선택된 워크스페이스의 프로젝트 목록
  const { data: projects = [], isLoading: projLoading } = useQuery({
    queryKey: ['sidebarProjects', selectedWorkspaceId],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${selectedWorkspaceId}/projects`);
      return res.data.data.content;
    },
    enabled: !!selectedWorkspaceId,
  });

  // 워크스페이스 전환: state + localStorage 동시 업데이트
  const handleWorkspaceSwitch = (wsPublicId) => {
    setSavedId(wsPublicId);
    localStorage.setItem(LAST_WORKSPACE_KEY, wsPublicId);
  };

  const handleNavigate = (key) => {
    nav(key);
    onClose?.();
  };

  return (
    <Flex vertical style={{ height: '100%', overflow: 'hidden' }}>
      <WorkspaceSwitcher
        user={user}
        workspaces={workspaces}
        selectedWorkspace={selectedWorkspace}
        isLoading={wsLoading}
        onSwitch={handleWorkspaceSwitch}
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