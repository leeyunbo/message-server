package com.readtimeout.presentation.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.inbound.ReactiveMessageSendUseCase;
import com.readtimeout.infrastructure.support.ConcurrencyLimiter;
import com.readtimeout.presentation.netty.HttpRequestRouter;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "reactive-pool")
public class ReactivePoolHttpServerHandler extends AbstractHttpServerHandler {

    private final ReactiveMessageSendUseCase useCase;
    private final ConcurrencyLimiter limiter;

    public ReactivePoolHttpServerHandler(
            HttpRequestRouter router,
            ObjectMapper objectMapper,
            ReactiveMessageSendUseCase useCase,
            ConcurrencyLimiter limiter,
            MeterRegistry meterRegistry) {
        super(router, objectMapper, meterRegistry);
        this.useCase = useCase;
        this.limiter = limiter;
    }

    @Override
    protected void processAndRespond(ChannelHandlerContext ctx, SendMessage sendMessage, String requestId) {
        try {
            limiter.acquire();
        } catch (Exception e) {
            sendPublishFailureResponse(ctx, sendMessage, e);
            return;
        }

        useCase.send(sendMessage)
                .doFinally(signal -> limiter.release())
                .doOnSuccess(v -> sendSuccessResponse(ctx, sendMessage, requestId))
                .doOnError(e -> sendPublishFailureResponse(ctx, sendMessage, e))
                .subscribe();
    }
}
