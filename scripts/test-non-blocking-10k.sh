#!/bin/bash

# V2: Non-blocking Mode Test - 10K VUs
# High load test to observe ThreadPool behavior under extreme concurrency

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

SERVER_URL="${SERVER_URL:-http://localhost:8081/api/message}"
INFLUXDB="${INFLUXDB:-true}"

echo "========================================="
echo "V2: Non-blocking Mode - 10K VUs Test"
echo "========================================="
echo "Target: $SERVER_URL"
echo "InfluxDB: $INFLUXDB"
echo ""
echo "Stages: 2K -> 5K -> 10K VUs (15min)"
echo ""
echo "Expected results:"
echo "  - ThreadPool may saturate"
echo "  - Memory usage increase"
echo "  - Compare with V3 Reactive"
echo "========================================="
echo ""

# Check if server is running
if ! curl -s "$SERVER_URL" > /dev/null 2>&1; then
    echo "Server not running. Starting with non-blocking mode..."
    echo ""
    echo "Run in another terminal:"
    echo "  ./gradlew :netty-server:bootRun --args='--server.mode=non-blocking'"
    echo ""
    exit 1
fi

# Run k6 test
if [ "$INFLUXDB" = "true" ]; then
    echo "Running with InfluxDB output for Grafana visualization..."
    k6 run --out influxdb=http://localhost:8086/k6 k6/non-blocking-10k-test.js
else
    k6 run k6/non-blocking-10k-test.js
fi

echo ""
echo "========================================="
echo "Test completed!"
echo "========================================="
echo "Check Grafana: http://localhost:3001"
echo "Check Prometheus: http://localhost:9091/metrics"
