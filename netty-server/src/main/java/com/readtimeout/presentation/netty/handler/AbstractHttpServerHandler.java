package com.readtimeout.presentation.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readtimeout.core.domain.exception.BackpressureRejectedException;
import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.presentation.netty.HttpRequestRouter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Sharable
public abstract class AbstractHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected final HttpRequestRouter router;
    protected final ObjectMapper objectMapper;
    protected final Counter httpRequestCounter;

    protected AbstractHttpServerHandler(
            HttpRequestRouter router,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.router = router;
        this.objectMapper = objectMapper;
        this.httpRequestCounter = Counter.builder("http.requests.total")
                .description("Total number of HTTP requests")
                .register(meterRegistry);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        httpRequestCounter.increment();

        HttpRequestRouter.Route route = router.route(request.method(), request.uri());

        if (route == null) {
            sendNotFound(ctx);
            return;
        }

        switch (route) {
            case PUBLISH_MESSAGE -> handlePublishMessage(ctx, request);
            case HEALTH_CHECK -> handleHealthCheck(ctx);
            default -> sendNotFound(ctx);
        }
    }

    private void handlePublishMessage(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            MessageRequest messageRequest = parseRequest(request);
            SendMessage sendMessage = createMessage(messageRequest);

            processAndRespond(ctx, sendMessage, messageRequest.requestId());
        } catch (IllegalArgumentException e) {
            sendBadRequest(ctx, e.getMessage());
        } catch (Exception e) {
            log.error("Error handling publish request: {}", e.getMessage(), e);
            sendBadRequest(ctx, "Invalid request: " + e.getMessage());
        }
    }

    protected abstract void processAndRespond(
            ChannelHandlerContext ctx,
            SendMessage sendMessage,
            String requestId);

    private MessageRequest parseRequest(FullHttpRequest request) throws Exception {
        String body = request.content().toString(StandardCharsets.UTF_8);
        Map<String, String> json = objectMapper.readValue(body, Map.class);

        String content = json.get("content");
        String requestId = json.get("requestId");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }

        return new MessageRequest(content, requestId);
    }

    private SendMessage createMessage(MessageRequest request) {
        String messageId = (request.requestId() != null && !request.requestId().isBlank())
                ? request.requestId()
                : UUID.randomUUID().toString();
        return new SendMessage(messageId, request.content());
    }

    protected void sendSuccessResponse(ChannelHandlerContext ctx, SendMessage sendMessage, String requestId) {
        sendJsonResponse(ctx, HttpResponseStatus.OK, Map.of(
                "status", "published",
                "messageId", sendMessage.getId(),
                "requestId", requestId != null ? requestId : ""
        ));

        log.debug("Message published successfully [id={}]", sendMessage.getId());
    }

    protected void sendPublishFailureResponse(ChannelHandlerContext ctx, SendMessage sendMessage, Throwable e) {
        Throwable cause = (e.getCause() != null) ? e.getCause() : e;
        log.error("Failed to send message [id={}]: {}", sendMessage.getId(), cause.getMessage(), cause);

        var errorResponse = switch (cause) {
            case BackpressureRejectedException bre ->
                    new ErrorResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, "rejected", "backpressure", bre.getMessage());
            case RejectedExecutionException ree ->
                    new ErrorResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, "rejected", "thread_pool_full", ree.getMessage());
            case MessagePublishException mpe ->
                    new ErrorResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, "failed", "publish_failed", mpe.getMessage());
            default ->
                    new ErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "error", null, "Internal server error");
        };

        sendJsonResponse(ctx, errorResponse.status(), errorResponse.toMap());
    }

    private void handleHealthCheck(ChannelHandlerContext ctx) {
        sendJsonResponse(ctx, HttpResponseStatus.OK, Map.of(
                "status", "UP",
                "architecture", "publisher-confirms"
        ));
    }

    private void sendNotFound(ChannelHandlerContext ctx) {
        sendTextResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
    }

    private void sendBadRequest(ChannelHandlerContext ctx, String message) {
        sendTextResponse(ctx, HttpResponseStatus.BAD_REQUEST, message);
    }

    protected void sendTextResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = createTextResponse(status, message);
        setTextHeaders(response);
        writeAndClose(ctx, response);
    }

    protected void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, Map<String, String> data) {
        try {
            FullHttpResponse response = createJsonResponse(status, data);
            setJsonHeaders(response);
            writeAndClose(ctx, response);
        } catch (Exception e) {
            log.error("Failed to send JSON response: {}", e.getMessage(), e);
            sendTextResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private FullHttpResponse createTextResponse(HttpResponseStatus status, String message) {
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)
        );
    }

    private FullHttpResponse createJsonResponse(HttpResponseStatus status, Map<String, String> data) throws Exception {
        String json = objectMapper.writeValueAsString(data);
        return new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
        );
    }

    private void setTextHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    private void setJsonHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    private void writeAndClose(ChannelHandlerContext ctx, FullHttpResponse response) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in handler: {}", cause.getMessage(), cause);
        ctx.close();
    }

    protected record MessageRequest(String content, String requestId) {
    }

    private record ErrorResponse(HttpResponseStatus status, String statusText, String reason, String message) {
        Map<String, String> toMap() {
            if (reason != null) {
                return Map.of("status", statusText, "reason", reason, "message", message);
            }
            return Map.of("status", statusText, "message", message);
        }
    }
}
