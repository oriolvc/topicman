package com.oriolvc.topicman.messaging.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.exception.InvalidDestinationException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import org.junit.jupiter.api.Test;

class DestinationTest {

    @Test
    void rejectsBlankTopicName() {
        assertThatThrownBy(() -> Destination.topic(" ")).isInstanceOf(InvalidDestinationException.class);
    }

    @Test
    void acceptsPulsarFullyQualifiedTopicName() {
        var topic = Destination.topic("persistent://public/default/orders");

        assertThat(topic.name()).isEqualTo("persistent://public/default/orders");
    }

    @Test
    void rejectsBlankExchangeName() {
        assertThatThrownBy(() -> Destination.exchange(" ", "orders.created"))
                .isInstanceOf(InvalidDestinationException.class);
    }

    @Test
    void allowsEmptyRoutingKeyForFanoutExchanges() {
        var exchange = Destination.exchange("events", "");

        assertThat(exchange.routingKey()).isEqualTo("");
    }

    @Test
    void tellsItsType() {
        var topic = Destination.topic("orders");
        var exchange = Destination.exchange("events", "orders.created");

        assertThat(topic.type()).isEqualTo(DestinationType.TOPIC);
        assertThat(exchange.type()).isEqualTo(DestinationType.EXCHANGE);
    }

    @Test
    void topicsAreSupportedByKafkaAndPulsarOnly() {
        var topic = Destination.topic("orders");

        assertThat(topic.isSupportedBy(BrokerType.KAFKA)).isTrue();
        assertThat(topic.isSupportedBy(BrokerType.PULSAR)).isTrue();
        assertThat(topic.isSupportedBy(BrokerType.RABBITMQ)).isFalse();
    }

    @Test
    void exchangesAreSupportedByRabbitMqOnly() {
        var exchange = Destination.exchange("events", "orders.created");

        assertThat(exchange.isSupportedBy(BrokerType.RABBITMQ)).isTrue();
        assertThat(exchange.isSupportedBy(BrokerType.KAFKA)).isFalse();
        assertThat(exchange.isSupportedBy(BrokerType.PULSAR)).isFalse();
    }
}
