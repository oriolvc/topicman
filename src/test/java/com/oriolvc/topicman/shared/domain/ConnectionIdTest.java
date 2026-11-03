package com.oriolvc.topicman.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConnectionIdTest {

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void wrapsAPositiveSequenceValue(long value) {
        assertThat(new ConnectionId(value).value()).isEqualTo(value);
        assertThat(new ConnectionId(value)).hasToString(String.valueOf(value));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsValuesASequenceNeverGives(long value) {
        assertThatThrownBy(() -> new ConnectionId(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
