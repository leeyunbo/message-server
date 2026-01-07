package com.readtimeout.core.application.dto;

import com.readtimeout.core.domain.exception.ValidationException;
import com.readtimeout.core.domain.model.RequestMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PublishRequest")
class PublishRequestTest {

    private RequestMetadata createValidMetadata() {
        return new RequestMetadata("req-123", "127.0.0.1", Instant.now());
    }

    @Nested
    @DisplayName("생성자 검증")
    class ConstructorValidation {

        @Test
        @DisplayName("유효한 입력으로 객체가 생성된다")
        void shouldCreateWithValidInputs() {
            // given
            RequestMetadata metadata = createValidMetadata();

            // when
            PublishRequest request = new PublishRequest("msg-123", "content", metadata);

            // then
            assertThat(request.getRequestId()).isEqualTo("msg-123");
            assertThat(request.getContent()).isEqualTo("content");
            assertThat(request.getMetadata()).isEqualTo(metadata);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("requestId가 null, 빈 값, 공백이면 ValidationException이 발생한다")
        void shouldThrowOnInvalidRequestId(String invalidRequestId) {
            // given
            RequestMetadata metadata = createValidMetadata();

            // when & then
            assertThatThrownBy(() -> new PublishRequest(invalidRequestId, "content", metadata))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Request ID");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("content가 null이거나 빈 값이면 ValidationException이 발생한다")
        void shouldThrowOnInvalidContent(String invalidContent) {
            // given
            RequestMetadata metadata = createValidMetadata();

            // when & then
            assertThatThrownBy(() -> new PublishRequest("msg-123", invalidContent, metadata))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Content");
        }

        @Test
        @DisplayName("metadata가 null이면 ValidationException이 발생한다")
        void shouldThrowOnNullMetadata() {
            // when & then
            assertThatThrownBy(() -> new PublishRequest("msg-123", "content", null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Metadata");
        }
    }

    @Nested
    @DisplayName("equals와 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("동일한 값을 가진 객체는 같다")
        void shouldBeEqualWithSameValues() {
            // given
            RequestMetadata metadata = createValidMetadata();
            PublishRequest request1 = new PublishRequest("msg-123", "content", metadata);
            PublishRequest request2 = new PublishRequest("msg-123", "content", metadata);

            // then
            assertThat(request1).isEqualTo(request2);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 객체는 다르다")
        void shouldNotBeEqualWithDifferentValues() {
            // given
            RequestMetadata metadata = createValidMetadata();
            PublishRequest request1 = new PublishRequest("msg-123", "content1", metadata);
            PublishRequest request2 = new PublishRequest("msg-456", "content2", metadata);

            // then
            assertThat(request1).isNotEqualTo(request2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringMethod {

        @Test
        @DisplayName("toString은 requestId와 contentLength를 포함한다")
        void shouldIncludeRequestIdAndContentLength() {
            // given
            RequestMetadata metadata = createValidMetadata();
            PublishRequest request = new PublishRequest("msg-123", "hello!!", metadata);

            // when
            String result = request.toString();

            // then
            assertThat(result).contains("msg-123");
            assertThat(result).contains("contentLength=7");
        }
    }
}
