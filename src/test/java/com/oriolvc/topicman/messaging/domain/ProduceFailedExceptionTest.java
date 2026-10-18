package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProduceFailedExceptionTest {

    @Test
    void carriesAStableCodeAndTheCause() {
        var cause = new RuntimeException("broker unreachable");

        var exception = new ProduceFailedException("Could not produce to orders", cause);

        assertThat(exception.code()).isEqualTo("messaging.produce-failed");
        assertThat(exception).hasMessage("Could not produce to orders").hasCause(cause);
        assertThat(exception.sent()).isZero();
    }

    @Test
    void tellsHowManyCopiesWereSentBeforeTheFailure() {
        var cause = new ProduceFailedException("Could not produce to orders", new RuntimeException());

        var exception = ProduceFailedException.afterSending(3, cause);

        assertThat(exception.sent()).isEqualTo(3);
        assertThat(exception).hasMessage("Could not produce to orders").hasCause(cause);
    }

    @Test
    void canHaveNoCause() {
        var exception = new ProduceFailedException("No producer for PULSAR");

        assertThat(exception).hasNoCause();
        assertThat(exception.sent()).isZero();
    }
}
