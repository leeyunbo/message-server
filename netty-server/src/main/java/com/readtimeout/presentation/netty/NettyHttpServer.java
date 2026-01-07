package com.readtimeout.presentation.netty;

import com.readtimeout.infrastructure.config.NettyProperties;
import com.readtimeout.presentation.netty.handler.AbstractHttpServerHandler;
import com.readtimeout.presentation.netty.metrics.EventLoopMetricsCollector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Netty HTTP Server
 *
 * Blocking/Non-blocking/Reactive 모드 모두 지원:
 * - AbstractHttpServerHandler를 통한 전략 패턴
 * - Spring이 server.mode에 따라 적절한 Handler 구현체 주입
 * - @Sharable 핸들러로 모든 연결에서 재사용
 */
@Component
public class NettyHttpServer {
    private static final Logger log = LoggerFactory.getLogger(NettyHttpServer.class);

    private final NettyProperties properties;
    private final AbstractHttpServerHandler handler;
    private final EventLoopMetricsCollector eventLoopMetrics;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyHttpServer(
            NettyProperties properties,
            AbstractHttpServerHandler handler,
            EventLoopMetricsCollector eventLoopMetrics) {
        this.properties = properties;
        this.handler = handler;
        this.eventLoopMetrics = eventLoopMetrics;
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                startServer();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Netty server startup interrupted", e);
            }
        }, "netty-server-starter").start();
    }

    private void startServer() throws InterruptedException {
        log.info("Starting Netty HTTP Server V2 (Non-blocking) on port {}", properties.httpPort());

        bossGroup = new NioEventLoopGroup(properties.bossThreads(),
                new DefaultThreadFactory("http-boss"));
        workerGroup = new NioEventLoopGroup(properties.workerThreads(),
                new DefaultThreadFactory("http-worker"));

        eventLoopMetrics.registerEventLoopGroup(workerGroup);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, properties.soBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, properties.keepAlive())
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast("httpCodec", new HttpServerCodec());
                            pipeline.addLast("aggregator",
                                    new HttpObjectAggregator(properties.maxContentLength()));
                            pipeline.addLast("handler", handler);
                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind(properties.httpPort()).sync();
            serverChannel = channelFuture.channel();

            log.info("Netty HTTP Server V2 started successfully on port {}", properties.httpPort());
            log.info("Architecture: Non-blocking (RabbitMQ on separate thread pool)");
            log.info("Boss Threads: {}, Worker Threads: {}",
                    properties.bossThreads(), properties.workerThreads());

            serverChannel.closeFuture().sync();

        } finally {
            log.info("Netty HTTP Server V2 shutting down");
            shutdown();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping Netty HTTP Server V2");

        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        log.info("Netty HTTP Server V2 stopped");
    }

    public boolean isRunning() {
        return serverChannel != null && serverChannel.isOpen();
    }
}
