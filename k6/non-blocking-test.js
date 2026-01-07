import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

/**
 * V2: Non-blocking Mode Test (Publisher Confirms)
 *
 * ThreadPool 기반 Publisher Confirms
 * - ThreadPoolExecutor에서 blocking waitForConfirms 실행
 * - Semaphore 기반 backpressure
 * - 200 = RabbitMQ 도달 확인됨
 * - 503 = Backpressure 또는 publish 실패
 */

// Custom Metrics - 실패 유형 분리
const successCount = new Counter('custom_success_total');
const backpressureCount = new Counter('custom_503_backpressure_total');
const timeoutCount = new Counter('custom_timeout_total');
const otherErrorCount = new Counter('custom_other_error_total');

const successDuration = new Trend('custom_success_duration');
const backpressureDuration = new Trend('custom_503_duration');

export const options = {
  stages: [
    { duration: '5m', target: 1000 },     // Ramp-up: 5분간 1000 VU까지
    { duration: '10m', target: 1000 },    // Steady: 10분간 1000 VU 유지
  ],
  thresholds: {
    'http_req_failed': ['rate<0.01'],           // 1% 미만 실패율
    'custom_503_backpressure_total': ['count==0'],  // 503 발생 시 실패
    'custom_timeout_total': ['count==0'],       // 타임아웃 발생 시 실패
  },
};

const SERVER_URL = __ENV.SERVER_URL || 'http://localhost:8081/api/message';

export default function () {
  const payload = JSON.stringify({
    content: `Non-blocking test - VU:${__VU} Iter:${__ITER}`,
    requestId: `nonblocking-${__VU}-${__ITER}`,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  const response = http.post(SERVER_URL, payload, params);

  // 실패 유형 분리 측정
  if (response.status === 200) {
    successCount.add(1);
    successDuration.add(response.timings.duration);
  } else if (response.status === 503) {
    backpressureCount.add(1);
    backpressureDuration.add(response.timings.duration);
  } else if (response.status === 0) {
    // status 0 = 타임아웃 또는 연결 실패
    timeoutCount.add(1);
  } else {
    otherErrorCount.add(1);
  }

  check(response, {
    'status is 200': (r) => r.status === 200,
    'no 503 error': (r) => r.status !== 503,
    'has messageId': (r) => r.status === 200 && r.json('messageId') !== undefined,
  });

  sleep(0.05);
}

export function setup() {
  console.log('Starting V2: Non-blocking Mode Test (Publisher Confirms)');
  console.log(`Target: ${SERVER_URL}`);
  console.log('Tech: ThreadPoolExecutor, RabbitTemplate.invoke, waitForConfirms');
  console.log('Response meaning:');
  console.log('  - 200 = Message confirmed by RabbitMQ');
  console.log('  - 503 = Backpressure or publish failed (retry later)');
}

export function teardown(data) {
  console.log('');
  console.log('========================================');
  console.log('V2: Non-blocking Mode Test Completed');
  console.log('========================================');
  console.log('Check Grafana for:');
  console.log('  - nonblocking_semaphore_available');
  console.log('  - p95/p99 latency');
  console.log('  - 503 rate (backpressure effectiveness)');
  console.log('========================================');
}
