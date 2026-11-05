package com.oriolvc.topicman.connection.application.model;

import com.oriolvc.topicman.connection.domain.model.Connection;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TrustedCertificates;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A connection as use cases return it: never with its credentials, only their type and the Kafka
 * SASL mechanism (ADR 0005, ADR 0008). TLS settings are not secret and are returned in full.
 *
 * @param saslMechanism set only for Kafka connections with a username and password
 * @param trustedCertificates CA certificates in PEM form, or null to trust the JVM's default CAs
 */
public record ConnectionResult(
        long id,
        String name,
        BrokerType brokerType,
        String address,
        CredentialsType credentialsType,
        @Nullable SaslMechanism saslMechanism,
        boolean tlsEnabled,
        @Nullable String trustedCertificates,
        Instant createdAt,
        Instant updatedAt) {

    public static ConnectionResult of(Connection connection) {
        return new ConnectionResult(
                connection.id().value(),
                connection.name().value(),
                connection.brokerType(),
                connection.address().value(),
                CredentialsType.of(connection.credentials()),
                connection.credentials() instanceof Credentials.UsernamePassword usernamePassword
                        ? usernamePassword.mechanism()
                        : null,
                connection.tls().enabled(),
                pemOf(connection.tls().trustedCertificates()),
                connection.createdAt(),
                connection.updatedAt());
    }

    public static @Nullable String pemOf(@Nullable TrustedCertificates certificates) {
        return certificates == null ? null : certificates.pem();
    }
}
