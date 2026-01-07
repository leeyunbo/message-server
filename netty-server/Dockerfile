# Multi-stage build for Netty Server

# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy Gradle files
COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./

# Copy source code
COPY netty-server-core/ netty-server-core/
COPY netty-server/ netty-server/

# Build
RUN ./gradlew :netty-server:build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/netty-server/build/libs/*.jar app.jar

# Expose ports
EXPOSE 8081 9091

# JMX ports (9010-9013)
EXPOSE 9010 9011 9012 9013

# Run with JAVA_OPTS support
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
