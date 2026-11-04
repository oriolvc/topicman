package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import org.jspecify.annotations.Nullable;

/**
 * How a connection uses TLS (ADR 0008). Kafka switches TLS on with {@code enabled}; Pulsar and
 * RabbitMQ with their address scheme ({@code pulsar+ssl://}, {@code amqps://}), so for them
 * {@code enabled} stays false and each broker has one place that says whether TLS is used. The
 * broker's certificate and hostname are always verified.
 *
 * @param trustedCertificates CA certificates to trust instead of the JVM's default CAs, or null to
 *     use the defaults
 */
public record TlsSettings(boolean enabled, @Nullable TrustedCertificates trustedCertificates) {

    private static final TlsSettings DISABLED = new TlsSettings(false, null);

    public static TlsSettings disabled() {
        return DISABLED;
    }

    /**
     * These settings, checked against the broker.
     *
     * @throws InvalidConnectionException if {@code enabled} is set for a broker that switches TLS on
     *     with its address scheme
     */
    public TlsSettings forBroker(BrokerType brokerType) {
        if (enabled) {
            switch (brokerType) {
                case KAFKA -> {}
                case PULSAR ->
                    throw new InvalidConnectionException(
                            "PULSAR switches TLS on with a pulsar+ssl:// address, not the TLS flag");
                case RABBITMQ ->
                    throw new InvalidConnectionException(
                            "RABBITMQ switches TLS on with an amqps:// address, not the TLS flag");
            }
        }
        return this;
    }
}
