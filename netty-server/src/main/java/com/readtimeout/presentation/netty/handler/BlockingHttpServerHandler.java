package com.readtimeout.presentation.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.inbound.MessageSendUseCase;
import com.readtimeout.presentation.netty.HttpRequestRouter;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.mode", havingValue = "blocking")
public class BlockingHttpServerHandler extends AbstractHttpServerHandler {

    private final MessageSendUseCase useCase;

    public BlockingHttpServerHandler(
            HttpRequestRouter router,
            ObjectMapper objectMapper,
            MessageSendUseCase useCase,
            MeterRegistry meterRegistry) {
        super(router, objectMapper, meterRegistry);
        this.useCase = useCase;
    }

    @Override
    protected void processAndRespond(ChannelHandlerContext ctx, SendMessage sendMessage, String requestId) {
        try {
            useCase.send(sendMessage);
            sendSuccessResponse(ctx, sendMessage, requestId);
        } catch (Exception e) {
            sendPublishFailureResponse(ctx, sendMessage, e);
        }
    }
}
