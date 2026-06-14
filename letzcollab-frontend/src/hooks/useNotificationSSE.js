import { useEffect, useRef, useState, useCallback } from 'react';

export function useNotificationSSE() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [latestNotification, setLatestNotification] = useState(null);

  const esRef = useRef(null);
  const reconnectTimerRef = useRef(null);
  const isMountedRef = useRef(true);

  const clearLatestNotification = useCallback(() => {
    setLatestNotification(null);
  }, []);

  const disconnect = useCallback(() => {
    isMountedRef.current = false;
    esRef.current?.close();
    esRef.current = null;

    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
  }, []);

  useEffect(() => {
    isMountedRef.current = true;

    const connect = async () => {
      if (!isMountedRef.current) return;

      const isProd = import.meta.env.PROD;
      const url = isProd
        ? '/api/v1/notifications/subscribe'
        : 'http://localhost:8080/api/v1/notifications/subscribe';

      const es = new EventSource(url, { withCredentials: true });
      esRef.current = es;

      es.onopen = () => {
        console.log('SSE 연결 성공');
      };

      // 최초 연결 - 미확인 알림 개수만 받음
      es.addEventListener('init', (event) => {
        try {
          const { unreadCount: count } = JSON.parse(event.data);
          if (isMountedRef.current) setUnreadCount(count ?? 0);
        } catch (e) {
          console.error('SSE init parse error', e);
        }
      });

      // SSE 연결 후, 실시간 알림을 받음
      es.addEventListener('notification', (event) => {
        try {
          const data = JSON.parse(event.data);
          if (isMountedRef.current) {
            setUnreadCount(data.unreadCount ?? 0);
            if (data.notification) setLatestNotification(data.notification);
          }
        } catch (e) {
          console.error('SSE notification parse error', e);
        }
      });

      es.onerror = async () => {
        es.close(); // 기존 에러 난 연결은 확실히 닫음
        esRef.current = null;

        if (!isMountedRef.current) return;

        // 이전 타이머 정리 후 새 타이머 설정 (중복 connect 방지)
        if (reconnectTimerRef.current) {
          clearTimeout(reconnectTimerRef.current);
        }
        // 1초 뒤에 다시 connect() 함수를 실행
        reconnectTimerRef.current = setTimeout(connect, 2000);
      };
    };

    connect();

    return () => {
      disconnect()
    };
  }, [disconnect]);

  return { unreadCount, latestNotification, clearLatestNotification, disconnect };
}