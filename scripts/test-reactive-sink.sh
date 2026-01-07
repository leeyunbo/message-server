#!/bin/bash

# V4: Reactive Sink Mode Test
# Run local server with reactive (sink) mode and execute k6 load test

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

SERVER_URL="${SERVER_URL:-http://localhost:8081/api/message}"
INFLUXDB="${INFLUXDB:-true}"

echo "========================================="
echo "V4: Reactive Sink Mode Load Test"
echo "========================================="
echo "Target: $SERVER_URL"
echo "InfluxDB: $INFLUXDB"
echo ""
echo "Tech Stack:"
echo "  - reactor-rabbitmq Sink pattern"
echo "  - per-EventLoop Sink"
echo "  - Fire-and-forget (emit and return)"
echo ""
echo "Expected results:"
echo "  - Ultra-low latency"
echo "  - Minimal blocking"
echo "  - High throughput"
echo "========================================="
echo ""

# Check if server is running
if ! curl -s "$SERVER_URL" > /dev/null 2>&1; then
    echo "Server not running. Starting with reactive (sink) mode..."
    echo ""
    echo "Run in another terminal:"
    echo "  ./gradlew :netty-server:bootRun --args='--server.mode=reactive'"
    echo ""
    echo "Or start and come back when ready."
    exit 1
fi

# Run k6 test
if [ "$INFLUXDB" = "true" ]; then
    echo "Running with InfluxDB output for Grafana visualization..."
    k6 run --out influxdb=http://localhost:8086/k6 k6/reactive-sink-test.js
else
    k6 run k6/reactive-sink-test.js
fi

echo ""
echo "========================================="
echo "Test completed!"
echo "========================================="
echo "Check Grafana: http://localhost:3001"
echo "Check Prometheus: http://localhost:9091/metrics"
