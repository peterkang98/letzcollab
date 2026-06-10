import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, tokens, commonSetup } from './common.js';
import exec from 'k6/execution';

// k6 run --out influxdb=http://localhost:8086/k6 k6-scripts/polling-notif.js

// 사용자 구성:
//   VU   1~300 → user_id   1~300 (알림 201개, 전부 미확인) - 1주 휴가 후 복귀
//   VU 301~400 → user_id 301~400 (알림  41개, 전부 미확인) - 바쁜 하루

export let options = {
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: 30,
      duration: '30s',
      startTime: '0s',
    },
    load_test: {
      executor: 'constant-vus',
      vus: 400,
      duration: '5m',
      startTime: '30s',
    }
  },
  thresholds: {
    'http_req_duration{scenario:load_test}': ['p(95) > 0']
  }
};

export function setup() {
  commonSetup();
}

export default function () {
  const vu = exec.vu;
  const scenarioName = vu.tags.scenario;

  let vuId;
  if (scenarioName === 'load_test') {
    vuId = vu.idInTest - 30;
  } else {
    vuId = vu.idInTest;
  }
  if (vuId <= 0) vuId = 1;
  const idx = (vuId - 1) % tokens.length;
  const token = tokens[idx];

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // 30초마다 polling (실제 프론트엔드 동작과 동일)
  const res = http.get(`${BASE_URL}/v1/notifications/unread-count`, { headers });

  check(res, {
    'notif status 200': (r) => r.status === 200,
  });

  sleep(30);
}
