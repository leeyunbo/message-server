package com.readtimeout.core.application.usecase;

import com.readtimeout.core.application.dto.PublishRequest;
import com.readtimeout.core.application.dto.PublishResponse;
import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.exception.ValidationException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.port.inbound.MessagePublishUseCase;
import com.readtimeout.core.domain.port.outbound.MessagePublisher;
import com.readtimeout.core.domain.port.outbound.MetricsCollector;
import com.readtimeout.core.domain.port.outbound.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PublishMessageUseCaseImpl implements MessagePublishUseCase {

    private final MessagePublisher messagePublisher;
    private final MetricsCollector metricsCollector;
    private final TimeProvider timeProvider;

    @Override
    public PublishResponse publishMessage(PublishRequest request) {
        long startTime = timeProvider.getCurrentTimeNanos();
        metricsCollector.incrementRequestCount();

        try {
            // 1. Domain 객체 생성
            SendMessage sendMessage = new SendMessage(request.getRequestId(), request.getContent());

            // 2. Fire-and-forget publish (즉시 반환)
            messagePublisher.publish(sendMessage);

            // 3. 제출 성공 → success 응답
            // (실제 RabbitMQ publish는 백그라운드에서 진행 중)
            long submissionLatency = timeProvider.getCurrentTimeNanos() - startTime;
            metricsCollector.recordPublishSuccess();
            metricsCollector.recordPublishLatency(submissionLatency);

            return PublishResponse.success(request.getRequestId(), submissionLatency);

        } catch (ValidationException e) {
            log.error("Validation failed: {}", e.getMessage());
            metricsCollector.recordPublishFailure();
            return PublishResponse.failure(request.getRequestId(), e.getMessage());

        } catch (MessagePublishException e) {
            // Queue overload or submission failure
            log.error("Message publish rejected: {}", e.getMessage(), e);
            metricsCollector.recordPublishFailure();
            return PublishResponse.failure(request.getRequestId(),
                    "Service overloaded: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during message publish", e);
            metricsCollector.recordPublishFailure();
            return PublishResponse.failure(request.getRequestId(),
                    "Internal server error");

        } finally {
            long duration = timeProvider.getCurrentTimeNanos() - startTime;
            metricsCollector.recordRequestProcessingTime(duration);
        }
    }
}
