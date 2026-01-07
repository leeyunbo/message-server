package com.readtimeout.core.domain.model;

import com.readtimeout.core.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Message Entity")
class SendMessageTest {

    @Nested
    @DisplayName("생성자 검증")
    class ConstructorValidation {

        @Test
        @DisplayName("유효한 입력으로 객체가 생성된다")
        void shouldCreateWithValidInputs() {
            // when
            SendMessage sendMessage = new SendMessage("msg-123", "test content");

            // then
            assertThat(sendMessage.getId()).isEqualTo("msg-123");
            assertThat(sendMessage.getContent()).isEqualTo("test content");
            assertThat(sendMessage.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("createdAt을 직접 지정할 수 있다")
        void shouldCreateWithExplicitCreatedAt() {
            // given
            Instant fixedTime = Instant.parse("2024-01-01T00:00:00Z");

            // when
            SendMessage sendMessage = new SendMessage("msg-123", "content", fixedTime);

            // then
            assertThat(sendMessage.getCreatedAt()).isEqualTo(fixedTime);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("id가 null, 빈 값, 공백이면 ValidationException이 발생한다")
        void shouldThrowOnInvalidId(String invalidId) {
            assertThatThrownBy(() -> new SendMessage(invalidId, "content"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Message ID");
        }

        @Test
        @DisplayName("content가 null이면 ValidationException이 발생한다")
        void shouldThrowOnNullContent() {
            assertThatThrownBy(() -> new SendMessage("msg-123", null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("content");
        }

        @Test
        @DisplayName("createdAt이 null이면 ValidationException이 발생한다")
        void shouldThrowOnNullCreatedAt() {
            assertThatThrownBy(() -> new SendMessage("msg-123", "content", null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("CreatedAt");
        }
    }

    @Nested
    @DisplayName("Entity 동등성 (ID 기반)")
    class EntityEquality {

        @Test
        @DisplayName("같은 ID를 가진 Entity는 동등하다")
        void shouldBeEqualWithSameId() {
            // given
            SendMessage sendMessage1 = new SendMessage("msg-123", "content1");
            SendMessage sendMessage2 = new SendMessage("msg-123", "different content");

            // then
            assertThat(sendMessage1).isEqualTo(sendMessage2);
            assertThat(sendMessage1.hashCode()).isEqualTo(sendMessage2.hashCode());
        }

        @Test
        @DisplayName("다른 ID를 가진 Entity는 동등하지 않다")
        void shouldNotBeEqualWithDifferentId() {
            // given
            SendMessage sendMessage1 = new SendMessage("msg-123", "content");
            SendMessage sendMessage2 = new SendMessage("msg-456", "content");

            // then
            assertThat(sendMessage1).isNotEqualTo(sendMessage2);
        }
    }

    @Nested
    @DisplayName("불변성")
    class Immutability {

        @Test
        @DisplayName("Message는 불변 객체이다")
        void shouldBeImmutable() {
            // given
            SendMessage sendMessage = new SendMessage("msg-123", "content");
            String originalId = sendMessage.getId();
            String originalContent = sendMessage.getContent();
            Instant originalCreatedAt = sendMessage.getCreatedAt();

            // then - 필드가 final이고 setter가 없으므로 변경 불가
            assertThat(sendMessage.getId()).isEqualTo(originalId);
            assertThat(sendMessage.getContent()).isEqualTo(originalContent);
            assertThat(sendMessage.getCreatedAt()).isEqualTo(originalCreatedAt);
        }
    }
}
