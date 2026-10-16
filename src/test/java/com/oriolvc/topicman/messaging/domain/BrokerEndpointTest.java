package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.shared.domain.BrokerType;
import org.junit.jupiter.api.Test;

class BrokerEndpointTest {

    @Test
    void keepsTheBrokerTypeAndAddress() {
        var endpoint = new BrokerEndpoint(BrokerType.KAFKA, "localhost:9092");

        assertThat(endpoint.type()).isEqualTo(BrokerType.KAFKA);
        assertThat(endpoint.address()).isEqualTo("localhost:9092");
    }

    @Test
    void rejectsBlankAddress() {
        assertThatThrownBy(() -> new BrokerEndpoint(BrokerType.KAFKA, " "))
                .isInstanceOf(InvalidBrokerEndpointException.class);
    }
}
