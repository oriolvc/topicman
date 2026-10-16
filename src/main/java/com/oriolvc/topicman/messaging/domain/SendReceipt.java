package com.oriolvc.topicman.messaging.domain;

import java.util.Objects;

/**
 * The broker's acknowledgement of a produced message, in the broker's own terms: a Kafka partition
 * and offset, a Pulsar message id, or a RabbitMQ publisher confirm.
 */
public record SendReceipt(String reference) {

    public SendReceipt {
        Objects.requireNonNull(reference, "reference");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("Send receipt reference must not be blank");
        }
    }
}
