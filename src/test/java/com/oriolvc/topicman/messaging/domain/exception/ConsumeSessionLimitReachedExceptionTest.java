package com.oriolvc.topicman.messaging.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConsumeSessionLimitReachedExceptionTest {

    @Test
    void carriesAStableCodeAndTheLimit() {
        var exception = new ConsumeSessionLimitReachedException(20);

        assertThat(exception.code()).isEqualTo("messaging.session-limit-reached");
        assertThat(exception).hasMessageContaining("20");
    }
}
