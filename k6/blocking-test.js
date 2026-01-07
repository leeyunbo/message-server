import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const status200 = new Counter('status_200');
const status503 = new Counter('status_503');
const status0 = new Counter('status_0_connection_error');
const statusOther = new Counter('status_other');

/**
 * Blocking Mode Test
 *
 * EventLoop 블로킹 현상 재현 및 측정
 * - 15% 확률로 20~800ms 블로킹 발생
 * - EventLoop Lag, Pending Tasks 증가 관찰
 * - p95/p99 latency 급격히 상승 예상
 */

export const options = {
  stages: [
    { duration: '5m', target: 1000 },     // Ramp-up: 5분간 1000 VU까지
    { duration: '10m', target: 1000 },    // Steady: 10분간 1000 VU 유지
  ],
  thresholds: {
    'http_req_failed': ['rate<0.01'],  // 1% 미만 실패율
    'status_503': ['count==0'],         // 503 발생 시 실패
  },
};

const SERVER_URL = __ENV.SERVER_URL || 'http://localhost:8081/api/message';

export default function () {
  const payload = JSON.stringify({
    content: `Blocking test - VU:${__VU} Iter:${__ITER}`,
    requestId: `blocking-${__VU}-${__ITER}`,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  const response = http.post(SERVER_URL, payload, params);

  if (response.status === 200) {
    status200.add(1);
  } else if (response.status === 503) {
    status503.add(1);
  } else if (response.status === 0) {
    status0.add(1);
  } else {
    statusOther.add(1);
  }

  check(response, {
    'status is 200': (r) => r.status === 200,
    'no 503 error': (r) => r.status !== 503,
    'has messageId': (r) => r.status === 200 && r.json('messageId') !== undefined,
  });

  sleep(0.05);  // 50ms think time - SAME AS OTHERS
}

export function setup() {
  console.log('Starting Blocking Mode Test (V1)');
  console.log(`Target: ${SERVER_URL}`);
  console.log('Expected: High latency, EventLoop blocking, cascading failures');
}

export function teardown(data) {
  console.log('Blocking Mode Test Completed');
  console.log('Check Grafana for:');
  console.log('  - EventLoop Lag (expected: high)');
  console.log('  - Pending Tasks (expected: 1000+)');
  console.log('  - p95/p99 latency (expected: >500ms)');
}
