package com.oriolvc.topicman.messaging.domain;

import org.jspecify.annotations.Nullable;

/**
 * How an adapter uses TLS (ADR 0008). {@code enabled} switches TLS on for Kafka, whose address has
 * no scheme; Pulsar and RabbitMQ switch it on with their address scheme instead. The broker's
 * certificate and hostname are always verified.
 *
 * @param trustedCertificates CA certificates in PEM form to trust instead of the JVM's default CAs,
 *     or null to use the defaults
 */
public record Tls(boolean enabled, @Nullable String trustedCertificates) {

    private static final Tls DEFAULTS = new Tls(false, null);

    /** No Kafka TLS, and the JVM's default CAs. */
    public static Tls defaults() {
        return DEFAULTS;
    }
}
