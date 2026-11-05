package com.oriolvc.topicman.connection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConnectionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-16T10:00:00Z");

    @Test
    void createsWithNameBrokerTypeAndAddress() {
        var connection = Connection.create(
                TestConnectionIds.next(),
                new ConnectionName("Local Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("localhost:9092"),
                Credentials.none(),
                CREATED_AT);

        assertThat(connection.name()).isEqualTo(new ConnectionName("Local Kafka"));
        assertThat(connection.brokerType()).isEqualTo(BrokerType.KAFKA);
        assertThat(connection.address()).isEqualTo(new BrokerAddress("localhost:9092"));
        assertThat(connection.credentials()).isEqualTo(Credentials.none());
        assertThat(connection.tls()).isEqualTo(TlsSettings.disabled());
        assertThat(connection.createdAt()).isEqualTo(CREATED_AT);
        assertThat(connection.updatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void createsWithTlsSettings() {
        var tls = new TlsSettings(true, new TrustedCertificates(Pem.CA));

        var connection = Connection.create(
                TestConnectionIds.next(),
                new ConnectionName("Secure Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("kafka.example.com:9093"),
                Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_256),
                tls,
                CREATED_AT);

        assertThat(connection.tls()).isEqualTo(tls);
    }

    @Test
    void defaultsKafkaUsernameAndPasswordToSaslPlain() {
        var connection = aKafkaConnection();

        connection.changeCredentials(Credentials.usernamePassword("app", new Secret("pw")), CREATED_AT);

        assertThat(connection.credentials())
                .isEqualTo(Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.PLAIN));
    }

    @Test
    void rejectsTheTlsFlagForABrokerThatUsesItsAddressScheme() {
        assertThatThrownBy(() -> Connection.create(
                        TestConnectionIds.next(),
                        new ConnectionName("Local Pulsar"),
                        BrokerType.PULSAR,
                        new BrokerAddress("pulsar://localhost:6650"),
                        Credentials.none(),
                        new TlsSettings(true, null),
                        CREATED_AT))
                .isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void changesTlsSettings() {
        var connection = aKafkaConnection();
        var later = CREATED_AT.plusSeconds(60);
        var tls = new TlsSettings(true, null);

        connection.changeTls(tls, later);

        assertThat(connection.tls()).isEqualTo(tls);
        assertThat(connection.updatedAt()).isEqualTo(later);
    }

    @Test
    void rejectsChangingToTlsSettingsTheBrokerDoesNotSupport() {
        var connection = Connection.create(
                TestConnectionIds.next(),
                new ConnectionName("Local RabbitMQ"),
                BrokerType.RABBITMQ,
                new BrokerAddress("amqp://localhost:5672"),
                Credentials.none(),
                CREATED_AT);

        assertThatThrownBy(() -> connection.changeTls(new TlsSettings(true, null), CREATED_AT.plusSeconds(60)))
                .isInstanceOf(InvalidConnectionException.class);
        assertThat(connection.tls()).isEqualTo(TlsSettings.disabled());
        assertThat(connection.updatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void rejectsCredentialsTheBrokerDoesNotSupport() {
        assertThatThrownBy(() -> Connection.create(
                        TestConnectionIds.next(),
                        new ConnectionName("Local RabbitMQ"),
                        BrokerType.RABBITMQ,
                        new BrokerAddress("amqp://localhost:5672"),
                        Credentials.token(new Secret("jwt")),
                        CREATED_AT))
                .isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void changesCredentials() {
        var connection = aKafkaConnection();
        var later = CREATED_AT.plusSeconds(60);
        var credentials = Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_256);

        connection.changeCredentials(credentials, later);

        assertThat(connection.credentials()).isEqualTo(credentials);
        assertThat(connection.updatedAt()).isEqualTo(later);
    }

    @Test
    void rejectsChangingToCredentialsTheBrokerDoesNotSupport() {
        var connection = aKafkaConnection();

        assertThatThrownBy(() ->
                        connection.changeCredentials(Credentials.token(new Secret("jwt")), CREATED_AT.plusSeconds(60)))
                .isInstanceOf(InvalidConnectionException.class);
        assertThat(connection.credentials()).isEqualTo(Credentials.none());
    }

    @Test
    void givesEachConnectionItsOwnIdentity() {
        var first = aKafkaConnection();
        var second = aKafkaConnection();

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void renames() {
        var connection = aKafkaConnection();
        var later = CREATED_AT.plusSeconds(60);

        connection.rename(new ConnectionName("Staging Kafka"), later);

        assertThat(connection.name()).isEqualTo(new ConnectionName("Staging Kafka"));
        assertThat(connection.updatedAt()).isEqualTo(later);
    }

    @Test
    void changesAddress() {
        var connection = aKafkaConnection();
        var later = CREATED_AT.plusSeconds(60);

        connection.changeAddress(new BrokerAddress("kafka-1:9092,kafka-2:9092"), later);

        assertThat(connection.address()).isEqualTo(new BrokerAddress("kafka-1:9092,kafka-2:9092"));
        assertThat(connection.updatedAt()).isEqualTo(later);
    }

    @Test
    void rehydratesStoredState() {
        var id = TestConnectionIds.next();
        var updatedAt = CREATED_AT.plusSeconds(60);

        var connection = Connection.rehydrate(
                id,
                new ConnectionName("Local Pulsar"),
                BrokerType.PULSAR,
                new BrokerAddress("pulsar://localhost:6650"),
                Credentials.token(new Secret("jwt")),
                new TlsSettings(false, new TrustedCertificates(Pem.CA)),
                CREATED_AT,
                updatedAt);

        assertThat(connection.id()).isEqualTo(id);
        assertThat(connection.name()).isEqualTo(new ConnectionName("Local Pulsar"));
        assertThat(connection.brokerType()).isEqualTo(BrokerType.PULSAR);
        assertThat(connection.address()).isEqualTo(new BrokerAddress("pulsar://localhost:6650"));
        assertThat(connection.credentials()).isEqualTo(Credentials.token(new Secret("jwt")));
        assertThat(connection.tls()).isEqualTo(new TlsSettings(false, new TrustedCertificates(Pem.CA)));
        assertThat(connection.createdAt()).isEqualTo(CREATED_AT);
        assertThat(connection.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void isIdentifiedByItsId() {
        var original = aKafkaConnection();

        var rehydrated = Connection.rehydrate(
                original.id(),
                new ConnectionName("Other name"),
                BrokerType.KAFKA,
                new BrokerAddress("other:9092"),
                Credentials.none(),
                TlsSettings.disabled(),
                CREATED_AT,
                CREATED_AT);

        assertThat(rehydrated).isEqualTo(original);
        assertThat(rehydrated).hasSameHashCodeAs(original);
    }

    private static Connection aKafkaConnection() {
        return Connection.create(
                TestConnectionIds.next(),
                new ConnectionName("Local Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("localhost:9092"),
                Credentials.none(),
                CREATED_AT);
    }
}
