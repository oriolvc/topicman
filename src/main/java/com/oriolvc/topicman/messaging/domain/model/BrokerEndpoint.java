package com.oriolvc.topicman.messaging.domain.model;

import com.oriolvc.topicman.messaging.domain.exception.InvalidBrokerEndpointException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/**
 * What an adapter needs to reach a broker: its type, address (Kafka bootstrap servers, Pulsar
 * service URL or AMQP URI), authentication and TLS settings (ADR 0008). The application layer
 * builds it from a stored connection. Its {@code toString()} never shows a secret.
 */
public record BrokerEndpoint(BrokerType type, String address, Authentication authentication, Tls tls) {

    public BrokerEndpoint {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(authentication, "authentication");
        Objects.requireNonNull(tls, "tls");
        if (address.isBlank()) {
            throw new InvalidBrokerEndpointException("Broker address must not be blank");
        }
    }

    /** An endpoint without authentication, with the default TLS settings. */
    public BrokerEndpoint(BrokerType type, String address) {
        this(type, address, Authentication.none(), Tls.defaults());
    }
}
