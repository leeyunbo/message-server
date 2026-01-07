import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

/**
 * V4: Reactive Sink Mode Test (Sink + Publisher Confirms)
 *
 * Sink 패턴 + Publisher Confirms
 * - per-EventLoop Sink: 스레드별 전용 무한 스트림
 * - Channel 재활용: 스트림 기반 효율적 채널 사용
 * - correlationId로 ACK/NACK 결과 매핑
 * - 200 = RabbitMQ ACK 확인됨
 * - 503 = Backpressure 또는 NACK/timeout
 */

// Error tracking counters
const status200 = new Counter('status_200');
const status503 = new Counter('status_503');
const status500 = new Counter('status_500');
const status0 = new Counter('status_0_connection_error');
const statusOther = new Counter('status_other');

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
    content: `Reactive Sink test - VU:${__VU} Iter:${__ITER}`,
    requestId: `reactive-sink-${__VU}-${__ITER}`,
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  const response = http.post(SERVER_URL, payload, params);

  // Track response status
  if (response.status === 200) {
    status200.add(1);
  } else if (response.status === 503) {
    status503.add(1);
  } else if (response.status === 500) {
    status500.add(1);
  } else if (response.status === 0) {
    status0.add(1);  // Connection error, timeout, reset
    console.log(`Connection error: ${response.error}`);
  } else {
    statusOther.add(1);
    console.log(`Unexpected status: ${response.status}, error: ${response.error}`);
  }

  check(response, {
    'status is 200': (r) => r.status === 200,
    'no 503 error': (r) => r.status !== 503,
    'has messageId': (r) => r.status === 200 && r.json('messageId') !== undefined,
  });

  sleep(0.05);
}

export function setup() {
  console.log('Starting V4: Reactive Sink Mode Test (Sink + Publisher Confirms)');
  console.log(`Target: ${SERVER_URL}`);
  console.log('Tech: per-EventLoop Sink, sendWithPublishConfirms, Channel reuse');
  console.log('Response meaning:');
  console.log('  - 200 = Message confirmed by RabbitMQ (ACK)');
  console.log('  - 503 = Backpressure or NACK/timeout (retry later)');
}

export function teardown(data) {
  console.log('V4: Reactive Sink Mode Test Completed');
  console.log('Check Grafana for:');
  console.log('  - reactive_sink_contexts (per-EventLoop sinks)');
  console.log('  - reactive_pending_requests (waiting for ACK)');
  console.log('  - p95/p99 latency');
}
