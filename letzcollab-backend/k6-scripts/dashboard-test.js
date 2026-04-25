import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data'

// 실행하기 전에 테스트 폴더에 있는 LoadTestDataGenerator 실행해서 tokens.txt랑 workspacePublicIds.txt를 미리 만들어야 함
// 프로젝트 루트에서 실행할 명령어: K6_WEB_DASHBOARD=true k6 run k6-scripts/dashboard-test.js
// p(95) 100ms ~ 500ms 목표

export let options = {
  stages: [
    { duration: '20s', target: 30 },   // 워밍업
    { duration: '2m', target: 400 },  // 부하
    { duration: '10s', target: 0 },    // 쿨다운
  ],
};

const BASE_URL = 'http://localhost:8080/api';

const tokens = new SharedArray('user tokens',
  () => open('../tokens.txt').split('\n').filter(t => t.trim().length > 0));
const workspaceIds = new SharedArray('workspace ids',
  () => open('../workspacePublicIds.txt').split('\n').filter(t => t.trim().length > 0));

export function setup() {
  console.log(`로드 완료: 토큰 ${tokens.length}개 / 워크스페이스ID ${workspaceIds.length}개`);
  // 데이터 개수가 맞지 않으면 경고 출력
  if (tokens.length !== workspaceIds.length) {
    console.warn('경고: 토큰과 워크스페이스 ID의 개수가 일치하지 않습니다!');
  }
}

// VU마다 반복 실행
export default function () {
  // VU 번호 기반으로 토큰 고정 할당 (같은 VU는 항상 같은 유저)
  const idx = (__VU - 1) % tokens.length;
  const token = tokens[idx];
  const workspacePublicId = workspaceIds[idx];

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  const res = http.get(`${BASE_URL}/v1/my/tasks?workspacePublicId=${workspacePublicId}`, { headers });

  check(res, {
    'status 200': r => r.status === 200,
    'has data': r => r.json('data') !== null,
  });

  sleep(1); // 요청 보내고 1초 쉼. Request Per Second가 총 VU 수와 동일하도록
}