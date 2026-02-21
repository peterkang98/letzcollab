import axios from 'axios';

// 매번 쿠키를 Cookie 헤더에 실어서 서버에 요청
const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  withCredentials: true
})

export default api;