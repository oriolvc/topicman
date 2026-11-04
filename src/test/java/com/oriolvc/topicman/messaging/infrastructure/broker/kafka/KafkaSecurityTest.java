package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.messaging.domain.Authentication;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Tls;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import org.apache.kafka.common.config.types.Password;
import org.junit.jupiter.api.Test;

class KafkaSecurityTest {

    private static final String PEM = "-----BEGIN CERTIFICATE-----\nMIIB\n-----END CERTIFICATE-----";

    @Test
    void usesPlaintextWithoutAuthenticationOrTls() {
        var config = KafkaSecurity.configFor(new BrokerEndpoint(BrokerType.KAFKA, "kafka:9092"));

        assertThat(config).containsOnlyKeys("security.protocol").containsEntry("security.protocol", "PLAINTEXT");
    }

    @Test
    void usesSslWithTlsAndVerifiesTheHostname() {
        var config = KafkaSecurity.configFor(endpoint(Authentication.none(), new Tls(true, null)));

        assertThat(config)
                .containsEntry("security.protocol", "SSL")
                .containsEntry("ssl.endpoint.identification.algorithm", "https")
                .doesNotContainKey("ssl.truststore.type");
    }

    @Test
    void trustsTheGivenCertificates() {
        var config = KafkaSecurity.configFor(endpoint(Authentication.none(), new Tls(true, PEM)));

        assertThat(config)
                .containsEntry("ssl.truststore.type", "PEM")
                .containsEntry("ssl.truststore.certificates", new Password(PEM));
    }

    @Test
    void ignoresTrustedCertificatesWithoutTls() {
        var config = KafkaSecurity.configFor(endpoint(Authentication.none(), new Tls(false, PEM)));

        assertThat(config).containsOnlyKeys("security.protocol").containsEntry("security.protocol", "PLAINTEXT");
    }

    @Test
    void sendsAUsernameAndPasswordWithSaslPlain() {
        var config = KafkaSecurity.configFor(endpoint(
                new Authentication.UsernamePassword("app", new Secret("pw"), SaslMechanism.PLAIN), Tls.defaults()));

        assertThat(config)
                .containsEntry("security.protocol", "SASL_PLAINTEXT")
                .containsEntry("sasl.mechanism", "PLAIN")
                .containsEntry(
                        "sasl.jaas.config",
                        new Password("org.apache.kafka.common.security.plain.PlainLoginModule required"
                                + " username=\"app\" password=\"pw\";"));
    }

    @Test
    void sendsAUsernameAndPasswordWithScramOverTls() {
        var config = KafkaSecurity.configFor(endpoint(
                new Authentication.UsernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_512),
                new Tls(true, null)));

        assertThat(config)
                .containsEntry("security.protocol", "SASL_SSL")
                .containsEntry("sasl.mechanism", "SCRAM-SHA-512")
                .containsEntry(
                        "sasl.jaas.config",
                        new Password("org.apache.kafka.common.security.scram.ScramLoginModule required"
                                + " username=\"app\" password=\"pw\";"));
    }

    @Test
    void defaultsToSaslPlainWithoutAMechanism() {
        var config = KafkaSecurity.configFor(
                endpoint(new Authentication.UsernamePassword("app", new Secret("pw"), null), Tls.defaults()));

        assertThat(config).containsEntry("sasl.mechanism", "PLAIN");
    }

    @Test
    void escapesQuotesAndBackslashesInTheJaasConfig() {
        var config = KafkaSecurity.configFor(endpoint(
                new Authentication.UsernamePassword("a\"pp", new Secret("p\\w\";x"), SaslMechanism.SCRAM_SHA_256),
                Tls.defaults()));

        assertThat(config)
                .containsEntry(
                        "sasl.jaas.config",
                        new Password("org.apache.kafka.common.security.scram.ScramLoginModule required"
                                + " username=\"a\\\"pp\" password=\"p\\\\w\\\";x\";"));
    }

    @Test
    void neverShowsThePasswordWhenTheConfigIsPrinted() {
        var config = KafkaSecurity.configFor(endpoint(
                new Authentication.UsernamePassword("app", new Secret("pw-value"), SaslMechanism.PLAIN),
                Tls.defaults()));

        assertThat(config.toString()).doesNotContain("pw-value");
    }

    private static BrokerEndpoint endpoint(Authentication authentication, Tls tls) {
        return new BrokerEndpoint(BrokerType.KAFKA, "kafka:9093", authentication, tls);
    }
}
