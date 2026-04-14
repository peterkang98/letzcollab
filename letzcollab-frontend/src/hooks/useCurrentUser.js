import { useQuery } from '@tanstack/react-query';
import api from '../api/axios.js';

// 사용자 정보를 가져오는 커스텀 훅:
export function useCurrentUser() {
  return useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const res = await api.get('/users/me');
      return res.data.data;
    },
    staleTime: 1000 * 60 * 5, // 빈번한 백엔드 호출 방지
  });
}