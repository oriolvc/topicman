package com.oriolvc.topicman.messaging.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.shared.domain.BrokerType;
import org.junit.jupiter.api.Test;

class BrokerCapabilitiesTest {

    @Test
    void kafkaUsesTopicsReadsFromTheBeginningAndSupportsKeys() {
        var kafka = BrokerCapabilities.of(BrokerType.KAFKA);

        assertThat(kafka).isEqualTo(new BrokerCapabilities(BrokerType.KAFKA, DestinationType.TOPIC, true, true));
    }

    @Test
    void pulsarUsesTopicsReadsFromTheBeginningAndSupportsKeys() {
        var pulsar = BrokerCapabilities.of(BrokerType.PULSAR);

        assertThat(pulsar).isEqualTo(new BrokerCapabilities(BrokerType.PULSAR, DestinationType.TOPIC, true, true));
    }

    @Test
    void rabbitMqUsesExchangesReadsOnlyNewMessagesAndHasNoKeys() {
        var rabbitMq = BrokerCapabilities.of(BrokerType.RABBITMQ);

        assertThat(rabbitMq)
                .isEqualTo(new BrokerCapabilities(BrokerType.RABBITMQ, DestinationType.EXCHANGE, false, false));
    }

    @Test
    void supportsOnlyItsKindOfDestination() {
        var kafka = BrokerCapabilities.of(BrokerType.KAFKA);

        assertThat(kafka.supports(Destination.topic("orders"))).isTrue();
        assertThat(kafka.supports(Destination.exchange("orders", "created"))).isFalse();
    }
}
