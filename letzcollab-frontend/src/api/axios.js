import axios from 'axios';

// 매번 쿠키를 Cookie 헤더에 실어서 서버에 요청
const api = axios.create({
  baseURL: import.meta.env.PROD ? '/api/v1' : 'http://localhost:8080/api/v1',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

let isRefreshing = false;           // jwt 재발급 중복 호출 방지
let failedQueue = [];               // jwt 재발급 중에 들어온 요청들 대기열

const processQueue = (error) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve();
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const errorCode = err.response?.data?.errorCode;
    const originalRequest = err.config;

    // A003: accessToken 만료 -> 재발급 시도
    if ((errorCode === 'A003' || errorCode === 'A001') && !originalRequest._retry) {
      originalRequest._retry = true;  // 무한 루프 방지

      // 앞에 있던 요청이 이미 jwt를 재발급하고 있는 중이라면 지금 요청은 대기열에 추가되고,
      // jwt 재발급이 성공하면 대기열에 있던 요청들이 다 resolve()됨 -> api(originalRequest) 요청 재시도
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      // 가장 먼저 API를 호출했다가 토큰 만료 에러를 받은 요청이 토큰 재발급을 수행
      isRefreshing = true;

      try {
        await api.post('/auth/refresh', null, {
          headers: { 'X-Client-Type': 'web' }
        });

        processQueue(null);
        return api(originalRequest);  // 원래 요청 재시도
      } catch (refreshError) {
        // JWT 재발급을 실패하면 로그인 페이지로
        processQueue(refreshError);
        localStorage.removeItem('user');
        window.location.replace('/auth/login');
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // A008: refreshToken도 만료 -> 로그인 페이지로
    if (errorCode === 'A008') {
      localStorage.removeItem('user');
      window.location.replace('/auth/login');
    }

    return Promise.reject(err);
  }
);

export default api;
