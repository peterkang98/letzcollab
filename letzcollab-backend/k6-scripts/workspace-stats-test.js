import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, tokens, workspaceIds, commonSetup} from "./common.js";
import exec from 'k6/execution';

// 실행하기 전에 테스트 폴더에 있는 LoadTestDataGenerator 실행해서 tokens.txt랑 workspacePublicIds.txt를 미리 만들어야 함
// 프로젝트 루트에서 실행할 명령어: K6_WEB_DASHBOARD=true k6 run k6-scripts/workspace-stats-test.js
// p(95) 100ms ~ 500ms 목표

export let options = {
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: 30,
      duration: '20s',
      startTime: '0s',
    },
    load_test: {
      executor: 'constant-vus',
      vus: 300,
      duration: '3m',
      startTime: '20s', // 워밍업이 끝난 후 시작
    }
  },
  // 부하 테스트 지표만 따로 보기: 테스트가 실패하지 않도록 무조건 참이 되는 조건을 값으로 넣었음
  thresholds: {
    'http_req_duration{scenario:load_test}': ['p(95) > 0']
  }
};

export function setup() {
  commonSetup();
}

// VU마다 반복 실행
export default function () {
  // VU 번호 기반으로 토큰 고정 할당 (같은 VU는 항상 같은 유저)
  const vu = exec.vu;
  const scenarioName = vu.tags.scenario; // 로그에 찍힌 tags.scenario 사용

  let vuId;

  if (scenarioName === 'load_test') {
    // load_test의 VU들은 31번부터 시작하므로, 30을 빼서 1번부터 시작하게 만듦
    vuId = vu.idInTest - 30;
  } else {
    // warmup 시나리오는 그대로 1~30 사용
    vuId = vu.idInTest;
  }

  // 안전장치: 혹시라도 0 이하가 되면 1로 고정
  if (vuId <= 0) vuId = 1;
  const idx = (vuId - 1) % tokens.length;
  const token = tokens[idx];
  const workspacePublicId = workspaceIds[idx];

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  const res = http.get(`${BASE_URL}/v1/workspaces/${workspacePublicId}/stats`, { headers });

  check(res, {
    'status 200': r => r.status === 200,
    'has data': r => r.json('data') !== null,
  });

  sleep(1); // 요청 보내고 1초 쉼. Request Per Second가 총 VU 수와 동일하도록
}