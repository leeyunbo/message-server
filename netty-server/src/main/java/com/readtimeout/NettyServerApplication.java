package com.readtimeout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Netty Server Application
 *
 * 서버 모드 설정 (application.yml의 server.mode):
 * - blocking: EventLoop Thread에서 직접 블로킹 작업 수행 (재현용)
 * - non-blocking: 별도 Thread Pool에서 비동기 처리 (개선 버전)
 *
 * Non-blocking 모드의 특징:
 * - RabbitMQ Publish를 별도 Thread Pool에서 비동기 처리
 * - EventLoop Thread는 즉시 반환하여 Blocking 방지
 * - CompletableFuture 기반 비동기 응답 처리
 *
 * 목표:
 * - EventLoop Pending Tasks 최소화 (0~10 유지)
 * - 높은 처리량 (10,000+ RPS)
 * - 낮은 Latency (p99 < 100ms)
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.readtimeout")
@ComponentScan(basePackages = {
        "com.readtimeout.core",  // Core domain
        "com.readtimeout"        // Infrastructure
})
public class NettyServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NettyServerApplication.class, args);
    }
}
