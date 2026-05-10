import { Flex, Typography } from 'antd';
import { STATUS_CONFIG } from '../../constants/taskStatus.js';

const { Text } = Typography;

export default function TaskDistributionBar({ stats }) {
  const segments = Object.values(STATUS_CONFIG)
    .map(cfg => ({ ...cfg, value: stats[cfg.dtoKey] ?? 0 }))
    .filter(s => s.value > 0)
    .sort((a, b) => a.order - b.order);

  const total = segments.reduce((sum, s) => sum + s.value, 0);
  if (total === 0) return null;

  return (
    <Flex vertical gap={8}>
      <Flex style={{ height: 8, borderRadius: 4, overflow: 'hidden', background: '#f0f0f0' }}>
        {segments.map((seg, i) => (
          <div
            key={i}
            style={{
              width: `${(seg.value / total) * 100}%`,
              background: seg.chartColor,
              transition: 'width 0.4s ease',
            }}
          />
        ))}
      </Flex>
      <Flex gap={12} wrap="wrap">
        {segments.map((seg, i) => (
          <Flex key={i} align="center" gap={4}>
            <div style={{ width: 8, height: 8, borderRadius: 2, background: seg.chartColor, flexShrink: 0 }} />
            <Text type="secondary" style={{ fontSize: 11 }}>{seg.label} {seg.value}</Text>
          </Flex>
        ))}
      </Flex>
    </Flex>
  );
}