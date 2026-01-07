package com.readtimeout.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus Metrics Collector V2
 */
@Configuration
public class PrometheusMetricsCollector {
    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsCollector.class);

    @Value("${prometheus.port:9091}")
    private int prometheusPort;

    @Value("${server.mode:unknown}")
    private String serverMode;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Bean
    public MeterRegistry meterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // Add common tags for all metrics
        registry.config().commonTags("mode", serverMode);

        new JvmThreadMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);

        log.info("Prometheus MeterRegistry initialized with mode={}", serverMode);
        return registry;
    }

    @PostConstruct
    public void startPrometheusServer() {
        MeterRegistry registry = meterRegistry();

        new Thread(() -> {
            try {
                bossGroup = new NioEventLoopGroup(1);
                workerGroup = new NioEventLoopGroup(2);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                pipeline.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                                        if (request.uri().equals("/metrics")) {
                                            String metrics = ((PrometheusMeterRegistry) registry).scrape();

                                            FullHttpResponse response = new DefaultFullHttpResponse(
                                                    HttpVersion.HTTP_1_1,
                                                    HttpResponseStatus.OK,
                                                    Unpooled.copiedBuffer(metrics, CharsetUtil.UTF_8)
                                            );

                                            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                                            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

                                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                        } else {
                                            FullHttpResponse response = new DefaultFullHttpResponse(
                                                    HttpVersion.HTTP_1_1,
                                                    HttpResponseStatus.NOT_FOUND,
                                                    Unpooled.copiedBuffer("Not Found", CharsetUtil.UTF_8)
                                            );
                                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                        }
                                    }
                                });
                            }
                        });

                ChannelFuture future = bootstrap.bind(prometheusPort).sync();
                serverChannel = future.channel();

                log.info("Prometheus metrics server started on port {}", prometheusPort);

                serverChannel.closeFuture().sync();
            } catch (Exception e) {
                log.error("Failed to start Prometheus server", e);
            } finally {
                shutdownPrometheusServer();
            }
        }, "prometheus-server").start();
    }

    @PreDestroy
    public void shutdownPrometheusServer() {
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("Prometheus metrics server stopped");
    }
}
