package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Instant;

/** A connection as use cases return it: never with its credentials, only their type (ADR 0005). */
public record ConnectionResult(
        long id,
        String name,
        BrokerType brokerType,
        String address,
        CredentialsType credentialsType,
        Instant createdAt,
        Instant updatedAt) {

    static ConnectionResult of(Connection connection) {
        return new ConnectionResult(
                connection.id().value(),
                connection.name().value(),
                connection.brokerType(),
                connection.address().value(),
                CredentialsType.of(connection.credentials()),
                connection.createdAt(),
                connection.updatedAt());
    }
}
