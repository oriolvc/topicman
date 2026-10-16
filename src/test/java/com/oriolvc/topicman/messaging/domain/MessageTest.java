package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void createsMessageWithoutKeyOrHeaders() {
        var message = Message.of(Payload.json("{\"orderId\":42}"));

        assertThat(message.key()).isNull();
        assertThat(message.headers()).isEqualTo(Headers.none());
    }

    @Test
    void addsKeyAndHeadersWithoutChangingTheOriginal() {
        var original = Message.of(Payload.text("hello"));

        var keyed = original.withKey("order-42").withHeaders(Headers.of(Map.of("trace-id", "abc")));

        assertThat(keyed.key()).isEqualTo("order-42");
        assertThat(keyed.headers().values()).containsEntry("trace-id", "abc");
        assertThat(original.key()).isNull();
    }

    @Test
    void rejectsEmptyKey() {
        var message = Message.of(Payload.text("hello"));

        assertThatThrownBy(() -> message.withKey("")).isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void encodesPayloadAsUtf8() {
        var payload = Payload.text("café");

        assertThat(payload.bytes()).isEqualTo("café".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void keepsThePayloadFormat() {
        assertThat(Payload.json("{}").format()).isEqualTo(PayloadFormat.JSON);
        assertThat(Payload.text("{}").format()).isEqualTo(PayloadFormat.TEXT);
    }
}
