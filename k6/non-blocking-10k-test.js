import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

/**
 * V2: Non-blocking Mode Test - 10K VUs
 *
 * 고부하 테스트: ThreadPool 기반 Publisher Confirms
 * - 10,000 VU로 ThreadPool 한계 테스트
 * - 스레드 풀 포화 시 503 발생 예상
 * - 메모리 사용량 관찰 필요
 */

const status200 = new Counter('status_200');
const status503 = new Counter('status_503');
const status0 = new Counter('status_0_connection_error');
const statusOther = new Counter('status_other');

const successDuration = new Trend('success_duration');
const backpressureDuration = new Trend('backpressure_duration');

export const options = {
  stages: [
    { duration: '2m', target: 2000 },    // Warm-up
    { duration: '3m', target: 5000 },    // Ramp to 5K
    { duration: '5m', target: 10000 },   // Ramp to 10K
    { duration: '5m', target: 10000 },   // Steady at 10K
  ],
  thresholds: {
    'http_req_failed': ['rate<0.01'],
    'status_503': ['count==0'],
  },
};

const SERVER_URL = __ENV.SERVER_URL || 'http://localhost:8081/api/message';

export default function () {
  const payload = JSON.stringify({
    content: `Non-blocking 10K test - VU:${__VU} Iter:${__ITER}`,
    requestId: `nb-10k-${__VU}-${__ITER}`,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  const response = http.post(SERVER_URL, payload, params);

  if (response.status === 200) {
    status200.add(1);
    successDuration.add(response.timings.duration);
  } else if (response.status === 503) {
    status503.add(1);
    backpressureDuration.add(response.timings.duration);
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

  sleep(0.05);
}

export function setup() {
  console.log('='.repeat(50));
  console.log('V2: Non-blocking Mode - 10K VUs Test');
  console.log('='.repeat(50));
  console.log(`Target: ${SERVER_URL}`);
  console.log('Stages: 2K -> 5K -> 10K VUs');
  console.log('');
  console.log('Expected behavior:');
  console.log('  - ThreadPool may saturate at high VUs');
  console.log('  - Memory usage will increase significantly');
  console.log('  - Watch for 503 backpressure responses');
  console.log('='.repeat(50));
}

export function teardown(data) {
  console.log('');
  console.log('='.repeat(50));
  console.log('V2: Non-blocking 10K Test Completed');
  console.log('='.repeat(50));
  console.log('Check metrics:');
  console.log('  - Thread count / memory usage');
  console.log('  - nonblocking_semaphore_available');
  console.log('  - 503 rate under extreme load');
  console.log('='.repeat(50));
}
