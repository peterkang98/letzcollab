import { useState } from 'react';
import { Badge, Button, Drawer, Empty, Flex, Pagination, Skeleton, Tag, Typography } from 'antd';
import { BellOutlined, CheckOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import { NOTIFICATION_TYPE_CONFIG, buildNotifLink } from '../constants/notificationConfig.js';
import { timeAgo } from '../utils/dateUtils.js';

const { Text } = Typography;

const PAGE_SIZE = 15;

export default function NotificationDrawer({ open, onClose }) {
  const nav = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1); // 1-based (UI용)

  const handleClose = () => {
    setPage(1);
    onClose();
  };

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', page],
    queryFn: async () => {
      const res = await api.get('/notifications', {
        params: {
          page: page - 1,       // 백엔드는 0-based
          size: PAGE_SIZE,
          sort: 'createdAt,desc',
        },
      });
      return res.data.data;
    },
    enabled: open,
    staleTime: 0,
  });

  // Spring Data Page 응답 구조: { content: [], page: { totalElements, totalPages, ... } }
  const notifications = data?.content ?? [];
  const totalElements = data?.page?.totalElements ?? 0;

  const { mutate: markOne } = useMutation({
    mutationFn: (id) => api.patch(`/notifications/${id}/read`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });

  const { mutate: markAll, isPending: isMarkingAll } = useMutation({
    mutationFn: () => api.patch('/notifications/read-all'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });

  const handleClick = (notif) => {
    if (!notif.isRead) markOne(notif.notificationId);
    const link = buildNotifLink(notif);
    if (link) {
      nav(link);
      handleClose();
    }
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <Drawer
      open={open}
      onClose={handleClose}
      size="default"
      placement="right"
      title={
        <Flex align="center" justify="space-between">
          <Flex align="center" gap={8}>
            <BellOutlined />
            <Text strong>알림</Text>
            {totalElements > 0 && (
              <Badge count={totalElements} overflowCount={99} size="small" />
            )}
          </Flex>
          {unreadCount > 0 && (
            <Button
              size="small"
              type="text"
              icon={<CheckOutlined />}
              onClick={() => markAll()}
              loading={isMarkingAll}
            >
              모두 읽음
            </Button>
          )}
        </Flex>
      }
      styles={{ body: { padding: 0, display: 'flex', flexDirection: 'column' } }}
    >
      {isLoading ? (
        <Flex vertical gap={0} style={{ padding: 16 }}>
          {[1, 2, 3, 4].map(i => (
            <Skeleton key={i} active paragraph={{ rows: 1 }} style={{ marginBottom: 12 }} />
          ))}
        </Flex>
      ) : !notifications.length ? (
        <Flex justify="center" align="center" style={{ flex: 1, minHeight: 300 }}>
          <Empty description="알림이 없습니다" />
        </Flex>
      ) : (
        <>
          {/* 알림 목록 */}
          <div style={{ flex: 1, overflowY: 'auto' }}>
            {notifications.map((notif) => {
              const typeInfo = NOTIFICATION_TYPE_CONFIG[notif.type] ?? { label: notif.type, color: 'default' };
              const link = buildNotifLink(notif);
              return (
                <div
                  key={notif.notificationId}
                  onClick={() => handleClick(notif)}
                  style={{
                    padding: '12px 16px',
                    cursor: link ? 'pointer' : 'default',
                    background: notif.isRead ? 'transparent' : 'rgba(24,144,255,0.05)',
                    borderLeft: notif.isRead ? '3px solid transparent' : '3px solid #1677ff',
                    borderBottom: '1px solid #f0f0f0',
                    transition: 'background 0.15s',
                  }}
                  onMouseEnter={e => { if (link) e.currentTarget.style.background = 'rgba(0,0,0,0.02)'; }}
                  onMouseLeave={e => { e.currentTarget.style.background = notif.isRead ? 'transparent' : 'rgba(24,144,255,0.05)'; }}
                >
                  <Flex vertical gap={4}>
                    <Flex justify="space-between" align="center">
                      <Tag color={typeInfo.color} style={{ margin: 0, fontSize: 11 }}>
                        {typeInfo.label}
                      </Tag>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        {timeAgo(notif.createdAt)}
                      </Text>
                    </Flex>
                    <Text style={{ fontSize: 13, color: notif.isRead ? '#8c8c8c' : '#262626' }}>
                      {notif.message}
                    </Text>
                  </Flex>
                </div>
              );
            })}
          </div>

          {/* 페이지네이션 — 총 알림이 PAGE_SIZE 초과 시에만 표시 */}
          {totalElements > PAGE_SIZE && (
            <Flex justify="center" style={{ padding: '12px 0', borderTop: '1px solid #f0f0f0', flexShrink: 0 }}>
              <Pagination
                current={page}
                total={totalElements}
                pageSize={PAGE_SIZE}
                onChange={(p) => setPage(p)}
                showSizeChanger={false}
                size="small"
              />
            </Flex>
          )}
        </>
      )}
    </Drawer>
  );
}