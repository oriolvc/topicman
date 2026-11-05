package com.oriolvc.topicman.messaging.domain.model;

import com.oriolvc.topicman.messaging.domain.exception.InvalidMessageException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A message to produce, or one that was read from a broker.
 *
 * @param key optional partitioning key (Kafka record key, Pulsar message key); RabbitMQ has none
 *     and routes with the destination's routing key instead
 */
public record Message(@Nullable String key, Headers headers, Payload payload) {

    public Message {
        if (key != null && key.isEmpty()) {
            throw new InvalidMessageException("Message key must not be empty; omit it instead");
        }
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(payload, "payload");
    }

    public static Message of(Payload payload) {
        return new Message(null, Headers.none(), payload);
    }

    public Message withKey(String key) {
        return new Message(key, headers, payload);
    }

    public Message withHeaders(Headers headers) {
        return new Message(key, headers, payload);
    }
}
