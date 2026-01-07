import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const status200 = new Counter('status_200');
const status503 = new Counter('status_503');
const status0 = new Counter('status_0_connection_error');
const statusOther = new Counter('status_other');

/**
 * V5: Virtual Thread Mode Test (Publisher Confirms)
 *
 * Java 21 Virtual Thread 기반 Publisher Confirms
 * - Virtual Thread: JVM이 관리하는 경량 스레드
 * - Semaphore 기반 backpressure
 * - 동기 코드 스타일로 waitForConfirms 호출
 * - 200 = RabbitMQ 도달 확인됨
 * - 503 = Backpressure 또는 publish 실패
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
    content: `Virtual thread test - VU:${__VU} Iter:${__ITER}`,
    requestId: `virtual-${__VU}-${__ITER}`,
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

  sleep(0.05);  // 50ms think time
}

export function setup() {
  console.log('Starting V5: Virtual Thread Mode Test (Publisher Confirms)');
  console.log(`Target: ${SERVER_URL}`);
  console.log('Tech: Java 21 Virtual Threads, waitForConfirms, Semaphore');
  console.log('Response meaning:');
  console.log('  - 200 = Message confirmed by RabbitMQ');
  console.log('  - 503 = Backpressure or publish failed (retry later)');
}

export function teardown(data) {
  console.log('V5: Virtual Thread Mode Test Completed');
  console.log('Check Grafana for:');
  console.log('  - virtual_thread_pending_tasks');
  console.log('  - virtual_thread_available_permits');
  console.log('  - p95/p99 latency');
  console.log('  - 503 rate (backpressure effectiveness)');
}
