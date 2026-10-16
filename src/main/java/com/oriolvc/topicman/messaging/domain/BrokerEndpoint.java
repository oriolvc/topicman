package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/**
 * What an adapter needs to reach a broker: its type and address (Kafka bootstrap servers, Pulsar
 * service URL or AMQP URI). The application layer builds it from a stored connection. Credentials
 * and TLS settings are added with the credential encryption task.
 */
public record BrokerEndpoint(BrokerType type, String address) {

    public BrokerEndpoint {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(address, "address");
        if (address.isBlank()) {
            throw new InvalidBrokerEndpointException("Broker address must not be blank");
        }
    }
}
