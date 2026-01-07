#!/bin/bash

# V5: Virtual Thread Mode Test
# Run local server with virtual thread mode and execute k6 load test

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

SERVER_URL="${SERVER_URL:-http://localhost:8081/api/message}"
INFLUXDB="${INFLUXDB:-true}"

echo "========================================="
echo "V5: Virtual Thread Mode Load Test"
echo "========================================="
echo "Target: $SERVER_URL"
echo "InfluxDB: $INFLUXDB"
echo ""
echo "Tech Stack:"
echo "  - Java 21+ Virtual Threads"
echo "  - Semaphore-based backpressure"
echo "  - Blocking code style, non-blocking performance"
echo ""
echo "Expected results:"
echo "  - Low latency (comparable to reactive)"
echo "  - High concurrency"
echo "  - Simple synchronous code"
echo "========================================="
echo ""

# Check if server is running
if ! curl -s "$SERVER_URL" > /dev/null 2>&1; then
    echo "Server not running. Starting with virtual thread mode..."
    echo ""
    echo "Run in another terminal:"
    echo "  ./gradlew :netty-server:bootRun --args='--server.mode=virtual'"
    echo ""
    echo "Or start and come back when ready."
    exit 1
fi

# Run k6 test
if [ "$INFLUXDB" = "true" ]; then
    echo "Running with InfluxDB output for Grafana visualization..."
    k6 run --out influxdb=http://localhost:8086/k6 k6/virtual-thread-test.js
else
    k6 run k6/virtual-thread-test.js
fi

echo ""
echo "========================================="
echo "Test completed!"
echo "========================================="
echo "Check Grafana: http://localhost:3001"
echo "Check Prometheus: http://localhost:9091/metrics"
