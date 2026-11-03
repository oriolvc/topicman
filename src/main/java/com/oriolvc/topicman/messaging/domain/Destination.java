package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/** Where a message is produced to or read from. Each broker supports one kind of destination. */
public sealed interface Destination permits Destination.Topic, Destination.Exchange {

    DestinationType type();

    default boolean isSupportedBy(BrokerType brokerType) {
        return BrokerCapabilities.of(brokerType).supports(this);
    }

    static Topic topic(String name) {
        return new Topic(name);
    }

    static Exchange exchange(String name, String routingKey) {
        return new Exchange(name, routingKey);
    }

    /**
     * A Kafka or Pulsar topic. Pulsar names may be short ({@code orders}) or fully qualified
     * ({@code persistent://tenant/namespace/orders}).
     */
    record Topic(String name) implements Destination {

        public Topic {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new InvalidDestinationException("Topic name must not be blank");
            }
        }

        @Override
        public DestinationType type() {
            return DestinationType.TOPIC;
        }
    }

    /**
     * A RabbitMQ exchange and routing key. The routing key may be empty, as fanout exchanges ignore
     * it. The default exchange is not supported, since consuming needs a named exchange to bind to.
     */
    record Exchange(String name, String routingKey) implements Destination {

        public Exchange {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(routingKey, "routingKey");
            if (name.isBlank()) {
                throw new InvalidDestinationException("Exchange name must not be blank");
            }
        }

        @Override
        public DestinationType type() {
            return DestinationType.EXCHANGE;
        }
    }
}
