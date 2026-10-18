package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReadFailedExceptionTest {

    @Test
    void carriesAStableCode() {
        var exception = new ReadFailedException("Topic 'orders' does not exist");

        assertThat(exception.code()).isEqualTo("messaging.read-failed");
        assertThat(exception).hasMessage("Topic 'orders' does not exist").hasNoCause();
    }

    @Test
    void carriesTheCause() {
        var cause = new RuntimeException("broker unreachable");

        var exception = new ReadFailedException("Could not read from orders", cause);

        assertThat(exception).hasCause(cause);
    }
}
