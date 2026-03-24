import { Select } from "antd";
import { useQuery } from "@tanstack/react-query";
import api from "../api/axios.js";

export default function WorkspaceSelect({ value, onChange }) {
  const { data, isLoading } = useQuery(
    {
      queryKey: ['workspaces'],
      queryFn: async () => {
        const res = await api.get('/workspaces');
        return res.data.data;
      }
    }
  );

  return (
    <Select
      style={{ width: 220 }}
      placeholder="워크스페이스 선택"
      loading={isLoading}
      options={(data ?? []).map(ws => ({ value: ws.publicId, label: ws.name }))}
      value={value}
      onChange={onChange}
      notFoundContent={isLoading ? '로딩 중' : '워크스페이스가 없습니다.'}
    />
  );
}