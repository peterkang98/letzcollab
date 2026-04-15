/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios.js';
import { LAST_WORKSPACE_KEY } from '../constants/storageKeys.js';

const WorkspaceContext = createContext(null);

export function WorkspaceProvider({ children }) {
  const [savedId, setSavedId] = useState(
    () => localStorage.getItem(LAST_WORKSPACE_KEY) ?? null
  );

  const { data: workspaces = [], isLoading: wsLoading } = useQuery({
    queryKey: ['workspaces'],
    queryFn: async () => {
      const res = await api.get('/workspaces');
      return res.data.data;
    },
  });

  const selectedWorkspace = workspaces.find((ws) => ws.publicId === savedId) ?? workspaces[0] ?? null;
  const selectedWorkspaceId = selectedWorkspace?.publicId ?? null;

  // 내 워크스페이스 권한 조회
  const { data: myWorkspaceMember } = useQuery({
    queryKey: ['workspaceMember', selectedWorkspaceId, 'me'],
    queryFn: async () => {
      const res = await api.get(`/workspaces/${selectedWorkspaceId}/members/me`);
      return res.data.data;
    },
    enabled: !!selectedWorkspaceId,
    staleTime: 0,
  });

  const myWorkspaceRole = myWorkspaceMember?.role ?? null;

  // selectedWorkspace 확정 시 localStorage에 동기화
  useEffect(() => {
    if (selectedWorkspaceId) {
      localStorage.setItem(LAST_WORKSPACE_KEY, selectedWorkspaceId);
    }
  }, [selectedWorkspaceId]);

  const switchWorkspace = (wsPublicId) => {
    setSavedId(wsPublicId);
  };

  return (
    <WorkspaceContext.Provider
      value={{ workspaces, wsLoading, selectedWorkspace, selectedWorkspaceId, switchWorkspace, myWorkspaceRole }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  const ctx = useContext(WorkspaceContext);
  if (!ctx) throw new Error('useWorkspace는 반드시 WorkspaceProvider 안에서 사용해야 합니다');
  return ctx;
}