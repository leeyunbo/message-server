#!/bin/bash

# Run Netty Server with specified mode
# Usage: ./scripts/run-server.sh [mode]
# Modes: blocking (V1), non-blocking (V2), reactive-pool (V3), reactive (V4), virtual (V5)

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

MODE="${1:-reactive}"
HEAP_SIZE="${HEAP_SIZE:-1g}"
JAR_PATH="netty-server/build/libs/netty-server-1.0.0.jar"

echo "========================================="
echo "Starting Netty Server"
echo "========================================="

case "$MODE" in
    blocking|v1)
        echo "V1: Blocking Mode (EventLoop blocking)"
        MODE="blocking"
        ;;
    non-blocking|v2)
        echo "V2: Non-blocking Mode (Thread Pool)"
        MODE="non-blocking"
        ;;
    reactive-pool|v3)
        echo "V3: Reactive Pool Mode (Channel Pool)"
        MODE="reactive-pool"
        ;;
    reactive|reactive-sink|v4)
        echo "V4: Reactive Sink Mode (Sink pattern)"
        MODE="reactive"
        ;;
    virtual|v5)
        echo "V5: Virtual Thread Mode (Java 21+)"
        MODE="virtual"
        ;;
    *)
        echo "Unknown mode: $MODE"
        echo ""
        echo "Available modes:"
        echo "  blocking     (V1) - EventLoop blocking"
        echo "  non-blocking (V2) - Thread Pool"
        echo "  reactive-pool(V3) - Channel Pool"
        echo "  reactive     (V4) - Sink pattern"
        echo "  virtual      (V5) - Virtual Threads"
        exit 1
        ;;
esac

echo "Heap Size: $HEAP_SIZE"
echo ""

# Always build to ensure latest code
echo "Building JAR..."
./gradlew :netty-server:bootJar -q
echo "Build complete!"
echo ""

echo "Server URL: http://localhost:8081/api/message"
echo "Prometheus: http://localhost:9091/metrics"
echo "========================================="
echo ""

# Run with separate JVM
exec java \
    -Xmx${HEAP_SIZE} \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -jar "$JAR_PATH" \
    --server.mode="$MODE"
