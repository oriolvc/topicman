package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import com.oriolvc.topicman.messaging.domain.model.Authentication;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.common.security.auth.SecurityProtocol;

/**
 * The client settings for an endpoint's TLS and SASL authentication (ADR 0008), shared by the
 * producer, the reader and the tester. Secrets are passed as {@link Password}, which Kafka never
 * prints: not in its config log, and not in this map's {@code toString()}.
 */
final class KafkaSecurity {

    private KafkaSecurity() {}

    /** The client settings with the endpoint's security settings added. */
    static Map<String, Object> withSecurity(BrokerEndpoint endpoint, Map<String, Object> clientConfig) {
        var config = new HashMap<>(clientConfig);
        config.putAll(configFor(endpoint));
        return config;
    }

    static Map<String, Object> configFor(BrokerEndpoint endpoint) {
        var tls = endpoint.tls();
        var sasl = endpoint.authentication() instanceof Authentication.UsernamePassword usernamePassword
                ? usernamePassword
                : null;
        var config = new HashMap<String, Object>();
        config.put("security.protocol", protocol(tls.enabled(), sasl != null).name);
        if (tls.enabled()) {
            // Kafka's default since 2.0, set so it cannot be switched off by accident.
            config.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
            var certificates = tls.trustedCertificates();
            if (certificates != null) {
                config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                config.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, new Password(certificates));
            }
        }
        if (sasl != null) {
            var mechanism = sasl.mechanism() == null ? SaslMechanism.PLAIN : sasl.mechanism();
            config.put(SaslConfigs.SASL_MECHANISM, mechanismName(mechanism));
            config.put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    new Password(loginModule(mechanism) + " required username=" + quoted(sasl.username()) + " password="
                            + quoted(sasl.password().value()) + ";"));
        }
        return config;
    }

    private static SecurityProtocol protocol(boolean tls, boolean sasl) {
        if (tls) {
            return sasl ? SecurityProtocol.SASL_SSL : SecurityProtocol.SSL;
        }
        return sasl ? SecurityProtocol.SASL_PLAINTEXT : SecurityProtocol.PLAINTEXT;
    }

    private static String mechanismName(SaslMechanism mechanism) {
        return switch (mechanism) {
            case PLAIN -> "PLAIN";
            case SCRAM_SHA_256 -> "SCRAM-SHA-256";
            case SCRAM_SHA_512 -> "SCRAM-SHA-512";
        };
    }

    private static String loginModule(SaslMechanism mechanism) {
        return switch (mechanism) {
            case PLAIN -> "org.apache.kafka.common.security.plain.PlainLoginModule";
            case SCRAM_SHA_256, SCRAM_SHA_512 -> "org.apache.kafka.common.security.scram.ScramLoginModule";
        };
    }

    /** A JAAS option value: quoted, with quotes and backslashes escaped, so no value can end it. */
    private static String quoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
