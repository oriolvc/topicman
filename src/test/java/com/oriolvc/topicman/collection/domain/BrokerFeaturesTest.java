package com.oriolvc.topicman.collection.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class BrokerFeaturesTest {

    private static final BrokerFeatures KAFKA = new BrokerFeatures(BrokerType.KAFKA, DestinationKind.TOPIC, true);
    private static final BrokerFeatures RABBITMQ =
            new BrokerFeatures(BrokerType.RABBITMQ, DestinationKind.EXCHANGE, false);

    @Test
    void acceptsATemplateThatFitsTheBroker() {
        assertThatCode(() -> KAFKA.check(aTemplate(BrokerType.KAFKA, TemplateDestination.topic("orders"), "k")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsADestinationKindTheBrokerDoesNotAccept() {
        assertThatThrownBy(
                        () -> RABBITMQ.check(aTemplate(BrokerType.RABBITMQ, TemplateDestination.topic("orders"), null)))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("RABBITMQ")
                .hasMessageContaining("EXCHANGE");
    }

    @Test
    void rejectsAKeyForABrokerWithoutKeys() {
        var template = aTemplate(BrokerType.RABBITMQ, TemplateDestination.exchange("events", ""), "order-42");

        assertThatThrownBy(() -> RABBITMQ.check(template))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("key");
    }

    @Test
    void rejectsATemplateForAnotherBroker() {
        assertThatThrownBy(() -> KAFKA.check(aTemplate(BrokerType.PULSAR, TemplateDestination.topic("orders"), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MessageTemplate aTemplate(
            BrokerType brokerType, TemplateDestination destination, @Nullable String key) {
        return MessageTemplate.create(
                new TemplateName("Template"),
                brokerType,
                destination,
                key,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "x"));
    }
}
