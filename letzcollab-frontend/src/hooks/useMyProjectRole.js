import { useQuery } from '@tanstack/react-query';
import api from '../api/axios.js';

// 내 프로젝트 역할 조회 훅
export function useMyProjectRole(projectPublicId) {
  return useQuery({
    queryKey: ['projectMember', projectPublicId, 'me'],
    queryFn: async () => {
      const res = await api.get(`/projects/${projectPublicId}/members/me`);
      return res.data.data; // { role: 'ADMIN' | 'MEMBER' | 'VIEWER', position: '...' }
    },
    enabled: !!projectPublicId,
    staleTime: 0,
  });
}