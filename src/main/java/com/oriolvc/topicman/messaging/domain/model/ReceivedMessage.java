package com.oriolvc.topicman.messaging.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A message read in a consume session.
 *
 * @param timestamp when the broker recorded the message
 * @param position where it was read, in the broker's terms (Kafka partition and offset, Pulsar
 *     message id, RabbitMQ delivery tag)
 */
public record ReceivedMessage(Message message, Instant timestamp, String position) {

    public ReceivedMessage {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(position, "position");
    }
}
