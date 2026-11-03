package com.oriolvc.topicman.collection.domain;

import java.util.Objects;

/**
 * Where a template's message goes: a topic, or an exchange and routing key. The texts may hold
 * placeholders until the template is resolved. Which broker accepts which kind is the messaging
 * context's rule, checked when a template is saved (ADR 0006).
 */
public sealed interface TemplateDestination permits TemplateDestination.Topic, TemplateDestination.Exchange {

    DestinationKind kind();

    static Topic topic(String name) {
        return new Topic(name);
    }

    static Exchange exchange(String name, String routingKey) {
        return new Exchange(name, routingKey);
    }

    record Topic(String name) implements TemplateDestination {

        public Topic {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new InvalidTemplateException("Topic name must not be blank");
            }
        }

        @Override
        public DestinationKind kind() {
            return DestinationKind.TOPIC;
        }
    }

    /** The routing key may be empty, as fanout exchanges ignore it. */
    record Exchange(String name, String routingKey) implements TemplateDestination {

        public Exchange {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(routingKey, "routingKey");
            if (name.isBlank()) {
                throw new InvalidTemplateException("Exchange name must not be blank");
            }
        }

        @Override
        public DestinationKind kind() {
            return DestinationKind.EXCHANGE;
        }
    }
}
