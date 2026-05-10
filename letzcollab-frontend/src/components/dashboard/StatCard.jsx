import { Card, Flex, Statistic, Typography } from 'antd';

const { Text } = Typography;

export default function StatCard({ icon, value, label, color = '#1677ff', suffix, }) {
  return (
    <Card
      variant={"borderless"}
      style={{ height: '100%' }}
      styles={{
        body: {
          padding: 16,
        },
      }}
    >
      <Flex align="center" gap={12}>
        {/* 아이콘 */}
        <Flex
          align="center"
          justify="center"
          style={{
            width: 40,
            height: 40,
            borderRadius: 10,
            background: `${color}15`,
            fontSize: 18,
            color,
            flexShrink: 0,
          }}
        >
          {icon}
        </Flex>

        {/* 텍스트 */}
        <Flex vertical gap={0}>
          <Statistic
            value={value}
            suffix={suffix}
            styles={{
              content: {
                fontSize: 20,
                fontWeight: 700,
                lineHeight: 1.1,
                color: '#262626',
                marginBottom: 4,
              },
            }}
          />

          <Text
            type="secondary"
            style={{
              fontSize: 12,
              lineHeight: 1.2,
            }}
          >
            {label}
          </Text>
        </Flex>
      </Flex>
    </Card>
  );
}