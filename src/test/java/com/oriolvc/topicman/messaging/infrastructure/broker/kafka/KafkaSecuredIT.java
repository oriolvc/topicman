package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.messaging.domain.Authentication;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.ConnectionTester;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.Tls;
import com.oriolvc.topicman.messaging.infrastructure.broker.SecuredBrokerContractTest;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.ScramCredentialInfo;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.apache.kafka.clients.admin.UserScramCredentialUpsertion;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * The Kafka adapter against a broker whose client listener requires SASL over TLS, with PLAIN and
 * both SCRAM mechanisms enabled (ADR 0008).
 */
@Testcontainers
class KafkaSecuredIT extends SecuredBrokerContractTest {

    private static final String KEYSTORE = "/etc/kafka/tls/server-keystore.pem";
    private static final String USER = "app";
    private static final String PASSWORD = "app-secret";

    // Testcontainers' client listener is named PLAINTEXT; only its security protocol changes.
    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1")
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:SASL_SSL,CONTROLLER:PLAINTEXT")
            .withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN,SCRAM-SHA-256,SCRAM-SHA-512")
            // The image maps ___ to - and _ to . in configuration names.
            .withEnv(
                    "KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required user_%s=\"%s\";"
                            .formatted(USER, PASSWORD))
            .withEnv(
                    "KAFKA_LISTENER_NAME_PLAINTEXT_SCRAM___SHA___256_SASL_JAAS_CONFIG",
                    "org.apache.kafka.common.security.scram.ScramLoginModule required;")
            .withEnv(
                    "KAFKA_LISTENER_NAME_PLAINTEXT_SCRAM___SHA___512_SASL_JAAS_CONFIG",
                    "org.apache.kafka.common.security.scram.ScramLoginModule required;")
            .withEnv("KAFKA_SSL_KEYSTORE_TYPE", "PEM")
            .withEnv("KAFKA_SSL_KEYSTORE_LOCATION", KEYSTORE)
            .withCopyToContainer(
                    Transferable.of(TestCertificates.pem("server-key.pem") + TestCertificates.pem("server.pem"), 0644),
                    KEYSTORE);

    private static final KafkaProperties PROPERTIES =
            new KafkaProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(3));

    private static Admin admin;

    @BeforeAll
    static void createScramUsers() throws Exception {
        var plain = new BrokerEndpoint(
                BrokerType.KAFKA,
                KAFKA.getBootstrapServers(),
                new Authentication.UsernamePassword(USER, new Secret(PASSWORD), SaslMechanism.PLAIN),
                new Tls(true, TestCertificates.CA));
        admin = Admin.create(
                KafkaSecurity.withSecurity(plain, Map.of("bootstrap.servers", KAFKA.getBootstrapServers())));
        // Kafka changes a user's credentials at most once per request.
        for (var mechanism : List.of(ScramMechanism.SCRAM_SHA_256, ScramMechanism.SCRAM_SHA_512)) {
            admin.alterUserScramCredentials(List.of(
                            new UserScramCredentialUpsertion(USER, new ScramCredentialInfo(mechanism, 4096), PASSWORD)))
                    .all()
                    .get();
        }
    }

    @AfterAll
    static void closeAdmin() {
        admin.close();
    }

    @Override
    protected MessageProducer createProducer() {
        return new KafkaMessageProducer(PROPERTIES);
    }

    @Override
    protected MessageReader createReader() {
        return new KafkaMessageReader(PROPERTIES);
    }

    @Override
    protected ConnectionTester createTester() {
        return new KafkaConnectionTester(PROPERTIES);
    }

    @Override
    protected BrokerEndpoint endpoint(Authentication authentication, @Nullable String trustedCertificates) {
        return new BrokerEndpoint(
                BrokerType.KAFKA, KAFKA.getBootstrapServers(), authentication, new Tls(true, trustedCertificates));
    }

    @Override
    protected Authentication validAuthentication() {
        return new Authentication.UsernamePassword(USER, new Secret(PASSWORD), SaslMechanism.PLAIN);
    }

    @Override
    protected Authentication rejectedAuthentication() {
        return new Authentication.UsernamePassword(USER, new Secret("wrong-secret"), SaslMechanism.PLAIN);
    }

    @Override
    protected Destination newDestination() {
        var topic = "secured-" + UUID.randomUUID();
        try {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return Destination.topic(topic);
    }

    @ParameterizedTest
    @EnumSource(SaslMechanism.class)
    void authenticatesWithEverySaslMechanism(SaslMechanism mechanism) {
        var endpoint = endpoint(
                new Authentication.UsernamePassword(USER, new Secret(PASSWORD), mechanism), TestCertificates.CA);

        var outcome = tester.test(endpoint);
        var receipt = producer.send(endpoint, newDestination(), Message.of(Payload.text(mechanism.name())));

        assertThat(outcome.succeeded()).as(outcome.detail()).isTrue();
        assertThat(receipt.reference()).endsWith("@0");
    }

    @ParameterizedTest
    @EnumSource(
            value = SaslMechanism.class,
            names = {"SCRAM_SHA_256", "SCRAM_SHA_512"})
    void rejectsAWrongScramPassword(SaslMechanism mechanism) {
        var endpoint = endpoint(
                new Authentication.UsernamePassword(USER, new Secret("wrong-secret"), mechanism), TestCertificates.CA);

        var outcome = tester.test(endpoint);

        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.detail()).doesNotContain("wrong-secret");
    }
}
