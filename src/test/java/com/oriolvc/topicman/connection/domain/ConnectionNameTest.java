package com.oriolvc.topicman.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConnectionNameTest {

    @Test
    void stripsSurroundingWhitespace() {
        var name = new ConnectionName("  Local Kafka  ");

        assertThat(name.value()).isEqualTo("Local Kafka");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new ConnectionName("   ")).isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void acceptsNameAtMaximumLength() {
        var name = new ConnectionName("a".repeat(ConnectionName.MAX_LENGTH));

        assertThat(name.value()).hasSize(ConnectionName.MAX_LENGTH);
    }

    @Test
    void rejectsNameLongerThanMaximumLength() {
        assertThatThrownBy(() -> new ConnectionName("a".repeat(ConnectionName.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidConnectionException.class);
    }
}
