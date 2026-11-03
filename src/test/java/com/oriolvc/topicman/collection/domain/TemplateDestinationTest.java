package com.oriolvc.topicman.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TemplateDestinationTest {

    @Test
    void keepsPlaceholdersInTheTopicName() {
        var topic = TemplateDestination.topic("orders-{{env}}");

        assertThat(topic).isEqualTo(new TemplateDestination.Topic("orders-{{env}}"));
    }

    @Test
    void tellsItsKind() {
        assertThat(TemplateDestination.topic("orders").kind()).isEqualTo(DestinationKind.TOPIC);
        assertThat(TemplateDestination.exchange("events", "").kind()).isEqualTo(DestinationKind.EXCHANGE);
    }

    @Test
    void rejectsABlankTopicName() {
        assertThatThrownBy(() -> TemplateDestination.topic(" ")).isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    void rejectsABlankExchangeName() {
        assertThatThrownBy(() -> TemplateDestination.exchange("", "created"))
                .isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    void allowsAnEmptyRoutingKeyForFanoutExchanges() {
        var exchange = TemplateDestination.exchange("events", "");

        assertThat(exchange.routingKey()).isEmpty();
    }
}
