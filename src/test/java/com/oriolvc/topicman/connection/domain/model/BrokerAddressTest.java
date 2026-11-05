package com.oriolvc.topicman.connection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import org.junit.jupiter.api.Test;

class BrokerAddressTest {

    @Test
    void stripsSurroundingWhitespace() {
        var address = new BrokerAddress(" localhost:9092 ");

        assertThat(address.value()).isEqualTo("localhost:9092");
    }

    @Test
    void rejectsBlankAddress() {
        assertThatThrownBy(() -> new BrokerAddress(" ")).isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void rejectsAddressLongerThanMaximumLength() {
        assertThatThrownBy(() -> new BrokerAddress("a".repeat(BrokerAddress.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidConnectionException.class);
    }
}
