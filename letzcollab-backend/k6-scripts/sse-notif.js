import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, tokens, commonSetup } from './common.js';
import exec from 'k6/execution';

// k6 run --out k6-scripts/sse-notif.js
//
// SSE 연결 유지 테스트
// - 각 VU는 1회의 장기 연결만 생성

export let options = {
  scenarios: {
    warmup: {
      executor: 'per-vu-iterations',
      vus: 30,
      iterations: 1,
      startTime: '0s',
    },
    load_test: {
      executor: 'per-vu-iterations',
      vus: 400,
      iterations: 1,
      startTime: '40s',
    }
  },
  thresholds: {
    'http_req_duration{scenario:load_test}': ['p(95) >= 0']
  }
};

export function setup() {
  commonSetup();
}

export default function () {
  let vuId = exec.vu.idInTest;
  let scenarioName = exec.scenario.name;

  const idx = (vuId - 1) % tokens.length;
  const token = tokens[idx];

  const headers = {
    'Accept': 'text/event-stream',
    'Authorization': `Bearer ${token}`,
  };

  const res = http.get(`${BASE_URL}/v1/notifications/subscribe-test`, {
    headers
  });

  check(res, {
    // 1. HTTP 상태 코드가 200인지 확인
    'SSE subscribe 200': (r) => r.status === 200,

    // 2. 응답 바디에 'init' 이벤트명이 포함되어 있는지 확인
    'Contains init event': (r) => r.body && r.body.includes('event:init'),

    // 3. 알림 개수 데이터 데이터가 실제로 들어왔는지 확인
    'Contains unreadCount data': (r) => r.body && r.body.includes('unreadCount'),
  });
}
