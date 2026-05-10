import { Empty, Flex, Progress, Typography } from 'antd';
import { PROJECT_STATUS_CONFIG } from '../../constants/projectStatus.js';

const { Text } = Typography;

export default function ProjectStatusList({ stats }) {
  const total = stats.total ?? 0;
  if (total === 0) return <Empty description="프로젝트가 없습니다" />;

  const rows = Object.values(PROJECT_STATUS_CONFIG)
    .map(cfg => ({ ...cfg, value: stats[cfg.dtoKey] ?? 0 }))
    .sort((a, b) => a.order - b.order);

  return (
    <Flex vertical gap={0}>
      {rows.map(({ dtoKey, chartColor, label, value }, i) => {
        const pct = Math.round((value / total) * 100);
        return (
          <Flex
            key={dtoKey}
            align="center"
            justify="space-between"
            style={{
              padding: '12px 0',
              borderBottom:
                i < rows.length - 1
                  ? '1px solid #f0f0f0'
                  : 'none',
            }}
          >
            {/* 상태명 */}
            <Flex align="center" gap={8}>
              <div
                style={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  background: chartColor,
                  flexShrink: 0,
                }}
              />

              <Text style={{ fontSize: 13 }}>
                {label}
              </Text>
            </Flex>

            {/* 퍼센트 + 개수 */}
            <Flex
              align="center"
              gap={10}
              style={{
                minWidth: 140,
              }}
            >
              <Progress
                percent={pct}
                showInfo={false}
                size="small"
                strokeColor={chartColor}
                railColor="#f0f0f0"
                style={{
                  margin: 0,
                  flex: 1,
                }}
              />

              <Text
                strong
                style={{
                  fontSize: 13,
                  minWidth: 18,
                  textAlign: 'right',
                }}
              >
                {value}
              </Text>
            </Flex>
          </Flex>
        );
      })}
    </Flex>
  );
}