package com.oriolvc.topicman.connection.domain.model;

import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import java.util.Objects;

/**
 * Where the broker is reached: Kafka bootstrap servers, Pulsar service URL or AMQP URI. Its format
 * is checked by the broker adapter when the connection is tested, so adding a broker does not
 * change the domain.
 */
public record BrokerAddress(String value) {

    public static final int MAX_LENGTH = 2048;

    public BrokerAddress {
        Objects.requireNonNull(value, "value");
        value = value.strip();
        if (value.isEmpty()) {
            throw new InvalidConnectionException("Broker address must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidConnectionException("Broker address must be at most " + MAX_LENGTH + " characters");
        }
    }
}
