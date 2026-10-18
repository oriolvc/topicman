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
    }
}
