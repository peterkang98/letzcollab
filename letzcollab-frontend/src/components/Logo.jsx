import { FolderOpenFilled } from "@ant-design/icons";
import { Flex } from "antd";

export default function Logo() {
  return (
    <Flex align="center" justify="center" gap={12} style={{ marginBottom: 32 }}>
      <FolderOpenFilled style={{ fontSize: '36px', color: '#1d1d1d' }}/>
      <span style={{
        fontSize: '36px',
        fontWeight: '850',
        letterSpacing: '-1.5px',
        color: '#1d1d1d'
      }}>
        Let'z Collab
      </span>
    </Flex>
  );
};