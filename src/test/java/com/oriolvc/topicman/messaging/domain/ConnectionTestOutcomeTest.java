package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConnectionTestOutcomeTest {

    @Test
    void succeedsWithWhatAnswered() {
        var outcome = ConnectionTestOutcome.succeeded("RabbitMQ 4.2.0");

        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.detail()).isEqualTo("RabbitMQ 4.2.0");
    }

    @Test
    void failsWithTheReason() {
        var outcome = ConnectionTestOutcome.failed("Connection refused");

        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.detail()).isEqualTo("Connection refused");
    }

    @Test
    void requiresADetail() {
        assertThatThrownBy(() -> ConnectionTestOutcome.failed(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
