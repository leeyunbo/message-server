package com.readtimeout.core.application.usecase;

import com.readtimeout.core.application.dto.PublishRequest;
import com.readtimeout.core.application.dto.PublishResponse;
import com.readtimeout.core.domain.exception.MessagePublishException;
import com.readtimeout.core.domain.model.SendMessage;
import com.readtimeout.core.domain.model.PublishStatus;
import com.readtimeout.core.domain.model.RequestMetadata;
import com.readtimeout.core.domain.port.outbound.MessagePublisher;
import com.readtimeout.core.domain.port.outbound.MetricsCollector;
import com.readtimeout.core.domain.port.outbound.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishMessageUseCaseImpl")
class PublishSendMessageUseCaseImplTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private MetricsCollector metricsCollector;

    @Mock
    private TimeProvider timeProvider;

    private PublishMessageUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PublishMessageUseCaseImpl(messagePublisher, metricsCollector, timeProvider);
    }

    private PublishRequest createValidRequest() {
        RequestMetadata metadata = new RequestMetadata("req-123", "127.0.0.1", Instant.now());
        return new PublishRequest("msg-123", "test content", metadata);
    }

    @Nested
    @DisplayName("publishMessage 성공 케이스")
    class PublishSendMessageSuccess {

        @Test
        @DisplayName("정상 요청 시 성공 응답을 반환한다")
        void shouldReturnSuccessResponse() {
            // given
            when(timeProvider.getCurrentTimeNanos()).thenReturn(0L, 1_000_000L);
            PublishRequest request = createValidRequest();

            // when
            PublishResponse response = useCase.publishMessage(request);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo(PublishStatus.OK);
            assertThat(response.getRequestId()).isEqualTo("msg-123");
            assertThat(response.getMqLatencyMs()).isEqualTo(1L);
        }

        @Test
        @DisplayName("메트릭이 올바르게 기록된다")
        void shouldRecordMetrics() {
            // given
            when(timeProvider.getCurrentTimeNanos()).thenReturn(0L, 1_000_000L);
            PublishRequest request = createValidRequest();

            // when
            useCase.publishMessage(request);

            // then
            verify(metricsCollector).incrementRequestCount();
            verify(metricsCollector).recordPublishSuccess();
            verify(metricsCollector).recordPublishLatency(anyLong());
            verify(metricsCollector).recordRequestProcessingTime(anyLong());
        }

        @Test
        @DisplayName("MessagePublisher.publish가 호출된다")
        void shouldCallPublisher() throws MessagePublishException {
            // given
            when(timeProvider.getCurrentTimeNanos()).thenReturn(0L, 1_000_000L);
            PublishRequest request = createValidRequest();

            // when
            useCase.publishMessage(request);

            // then
            verify(messagePublisher).publish(any(SendMessage.class));
        }
    }

    @Nested
    @DisplayName("publishMessage 실패 케이스")
    class PublishSendMessageFailure {

        @Test
        @DisplayName("MessagePublishException 발생 시 에러 응답을 반환한다")
        void shouldReturnErrorOnPublishException() throws MessagePublishException {
            // given
            when(timeProvider.getCurrentTimeNanos()).thenReturn(0L, 1_000_000L);
            doThrow(new MessagePublishException("Queue full"))
                    .when(messagePublisher).publish(any(SendMessage.class));
            PublishRequest request = createValidRequest();

            // when
            PublishResponse response = useCase.publishMessage(request);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo(PublishStatus.ERROR);
            assertThat(response.getErrorMessage()).contains("Queue full");
        }

        @Test
        @DisplayName("실패 시 실패 메트릭이 기록된다")
        void shouldRecordFailureMetrics() throws MessagePublishException {
            // given
            when(timeProvider.getCurrentTimeNanos()).thenReturn(0L, 1_000_000L);
            doThrow(new MessagePublishException("Error"))
                    .when(messagePublisher).publish(any(SendMessage.class));
            PublishRequest request = createValidRequest();

            // when
            useCase.publishMessage(request);

            // then
            verify(metricsCollector).recordPublishFailure();
            verify(metricsCollector, never()).recordPublishSuccess();
        }

        @Test
        @DisplayName("예상치 못한 예외 발생 시 에러 응답을 반환한다")
        void shouldReturnErrorOnUnexpectedException() throws MessagePublishException {
            // given
            when(timeProvider.getCurrentTimeNanos()).thenReturn(0L, 1_000_000L);
            doThrow(new RuntimeException("Unexpected"))
                    .when(messagePublisher).publish(any(SendMessage.class));
            PublishRequest request = createValidRequest();

            // when
            PublishResponse response = useCase.publishMessage(request);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorMessage()).isEqualTo("Internal server error");
        }
    }

    @Nested
    @DisplayName("TimeProvider 통합")
    class TimeProviderIntegration {

        @Test
        @DisplayName("TimeProvider를 사용하여 시간을 측정한다")
        void shouldUseTimeProvider() {
            // given
            when(timeProvider.getCurrentTimeNanos())
                    .thenReturn(1000L)  // startTime
                    .thenReturn(2000L)  // submissionLatency
                    .thenReturn(3000L); // duration in finally
            PublishRequest request = createValidRequest();

            // when
            useCase.publishMessage(request);

            // then
            verify(timeProvider, times(3)).getCurrentTimeNanos();
        }
    }
}
