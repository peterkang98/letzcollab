import axios from 'axios';

// 매번 쿠키를 Cookie 헤더에 실어서 서버에 요청
const api = axios.create({
  baseURL: import.meta.env.PROD ? '/api/v1' : 'http://localhost:8080/api/v1',
  withCredentials: true
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const errorCode = err.response?.data?.errorCode;
    if (errorCode === 'A003' || errorCode === 'A001') {
        localStorage.removeItem('user');
        window.location.replace('/auth/login');
    }

    return Promise.reject(err);
  }
)

export default api;