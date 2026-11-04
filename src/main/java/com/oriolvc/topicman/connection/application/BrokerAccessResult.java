package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.shared.domain.BrokerType;
import org.jspecify.annotations.Nullable;

/**
 * Everything a broker adapter needs to reach a connection's broker, decrypted credentials included
 * (ADR 0008). Never returned by the API.
 *
 * @param trustedCertificates CA certificates in PEM form, or null to trust the JVM's default CAs
 */
public record BrokerAccessResult(
        BrokerType brokerType,
        String address,
        BrokerCredentials credentials,
        boolean tlsEnabled,
        @Nullable String trustedCertificates) {

    static BrokerAccessResult of(Connection connection) {
        return new BrokerAccessResult(
                connection.brokerType(),
                connection.address().value(),
                credentialsOf(connection.credentials()),
                connection.tls().enabled(),
                ConnectionResult.pemOf(connection.tls().trustedCertificates()));
    }

    private static BrokerCredentials credentialsOf(Credentials credentials) {
        return switch (credentials) {
            case Credentials.None none -> BrokerCredentials.none();
            case Credentials.Token token -> new BrokerCredentials.Token(token.token());
            case Credentials.UsernamePassword usernamePassword ->
                new BrokerCredentials.UsernamePassword(
                        usernamePassword.username(), usernamePassword.password(), usernamePassword.mechanism());
        };
    }
}
