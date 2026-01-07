#!/bin/bash

# V3: Reactive Pool Mode Test
# Run local server with reactive-pool mode and execute k6 load test

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

SERVER_URL="${SERVER_URL:-http://localhost:8081/api/message}"
INFLUXDB="${INFLUXDB:-true}"

echo "========================================="
echo "V3: Reactive Pool Mode Load Test"
echo "========================================="
echo "Target: $SERVER_URL"
echo "InfluxDB: $INFLUXDB"
echo ""
echo "Tech Stack:"
echo "  - reactor-rabbitmq (NIO-based)"
echo "  - LazyChannelPool (Channel reuse)"
echo "  - Fire-and-forget pattern"
echo ""
echo "Expected results:"
echo "  - Ultra-low latency (p95 <100ms)"
echo "  - No EventLoop blocking"
echo "  - Efficient channel reuse"
echo "========================================="
echo ""

# Check if server is running
if ! curl -s "$SERVER_URL" > /dev/null 2>&1; then
    echo "Server not running. Starting with reactive-pool mode..."
    echo ""
    echo "Run in another terminal:"
    echo "  ./gradlew :netty-server:bootRun --args='--server.mode=reactive-pool'"
    echo ""
    echo "Or start and come back when ready."
    exit 1
fi

# Run k6 test
if [ "$INFLUXDB" = "true" ]; then
    echo "Running with InfluxDB output for Grafana visualization..."
    k6 run --out influxdb=http://localhost:8086/k6 k6/reactive-pool-test.js
else
    k6 run k6/reactive-pool-test.js
fi

echo ""
echo "========================================="
echo "Test completed!"
echo "========================================="
echo "Check Grafana: http://localhost:3001"
echo "Check Prometheus: http://localhost:9091/metrics"
