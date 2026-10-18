package com.oriolvc.topicman.connection.infrastructure.persistence;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.Secret;
import com.oriolvc.topicman.connection.infrastructure.crypto.CredentialDecryptionException;
import com.oriolvc.topicman.connection.infrastructure.crypto.CryptoConfiguration;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@DataJpaTest
@TestConstructor(autowireMode = AutowireMode.ALL)
@Import({
    TestcontainersConfiguration.class,
    CryptoConfiguration.class,
    CredentialsCodec.class,
    JpaConnectionRepository.class
})
class JpaConnectionRepositoryIT {

    private static final Instant CREATED_AT = Instant.parse("2026-10-16T10:00:00.123456Z");

    private final JpaConnectionRepository repository;
    private final EntityManager entityManager;

    JpaConnectionRepositoryIT(JpaConnectionRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsAConnection() {
        var connection = aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092");

        repository.save(connection);
        flushAndClear();

        assertThat(repository.findById(connection.id())).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(connection.id());
            assertThat(found.name()).isEqualTo(new ConnectionName("Local Kafka"));
            assertThat(found.brokerType()).isEqualTo(BrokerType.KAFKA);
            assertThat(found.address()).isEqualTo(new BrokerAddress("localhost:9092"));
            assertThat(found.credentials()).isEqualTo(Credentials.none());
            assertThat(found.createdAt()).isEqualTo(CREATED_AT);
            assertThat(found.updatedAt()).isEqualTo(CREATED_AT);
        });
    }

    @Test
    void updatesAnExistingConnection() {
        var connection = aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092");
        repository.save(connection);
        flushAndClear();
        var updatedAt = CREATED_AT.plus(1, ChronoUnit.HOURS);
        connection.rename(new ConnectionName("Staging Kafka"), updatedAt);
        connection.changeAddress(new BrokerAddress("kafka.staging:9092"), updatedAt);

        repository.save(connection);
        flushAndClear();

        assertThat(repository.findById(connection.id())).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo(new ConnectionName("Staging Kafka"));
            assertThat(found.address()).isEqualTo(new BrokerAddress("kafka.staging:9092"));
            assertThat(found.createdAt()).isEqualTo(CREATED_AT);
            assertThat(found.updatedAt()).isEqualTo(updatedAt);
        });
    }

    @Test
    void savesAndFindsATokenCredential() {
        var connection = aConnection("Local Pulsar", BrokerType.PULSAR, "pulsar://localhost:6650");
        connection.changeCredentials(Credentials.token(new Secret("jwt-value")), CREATED_AT);

        repository.save(connection);
        flushAndClear();

        assertThat(repository.findById(connection.id()))
                .hasValueSatisfying(
                        found -> assertThat(found.credentials()).isEqualTo(Credentials.token(new Secret("jwt-value"))));
    }

    @Test
    void savesAndFindsAUsernameAndPasswordCredential() {
        var connection = aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092");
        var credentials = Credentials.usernamePassword("app", new Secret("pw-value"));
        connection.changeCredentials(credentials, CREATED_AT);

        repository.save(connection);
        flushAndClear();

        assertThat(repository.findById(connection.id()))
                .hasValueSatisfying(found -> assertThat(found.credentials()).isEqualTo(credentials));
    }

    @Test
    void storesCredentialsEncrypted() {
        var connection = aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092");
        connection.changeCredentials(Credentials.usernamePassword("app-user", new Secret("pw-value")), CREATED_AT);

        repository.save(connection);
        flushAndClear();

        var row = (Object[]) entityManager
                .createNativeQuery(
                        "SELECT credentials_type, credentials_ciphertext FROM broker_connection WHERE id = :id")
                .setParameter("id", connection.id().value())
                .getSingleResult();
        assertThat(row[0]).isEqualTo("USERNAME_PASSWORD");
        assertThat(new String((byte[]) row[1], ISO_8859_1))
                .doesNotContain("app-user")
                .doesNotContain("pw-value");
    }

    @Test
    void clearsCiphertextWhenCredentialsAreRemoved() {
        var connection = aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092");
        connection.changeCredentials(Credentials.usernamePassword("app", new Secret("pw")), CREATED_AT);
        repository.save(connection);
        flushAndClear();
        connection.changeCredentials(Credentials.none(), CREATED_AT.plusSeconds(60));

        repository.save(connection);
        flushAndClear();

        assertThat(ciphertextOf(connection.id().value())).isNull();
        assertThat(repository.findById(connection.id()))
                .hasValueSatisfying(found -> assertThat(found.credentials()).isEqualTo(Credentials.none()));
    }

    @Test
    void rejectsCiphertextCopiedFromAnotherConnection() {
        var source = aConnection("Kafka A", BrokerType.KAFKA, "a:9092");
        source.changeCredentials(Credentials.usernamePassword("app", new Secret("pw")), CREATED_AT);
        var target = aConnection("Kafka B", BrokerType.KAFKA, "b:9092");
        target.changeCredentials(Credentials.usernamePassword("other", new Secret("other-pw")), CREATED_AT);
        repository.save(source);
        repository.save(target);
        flushAndClear();
        entityManager
                .createNativeQuery("UPDATE broker_connection SET credentials_ciphertext = :ciphertext WHERE id = :id")
                .setParameter("ciphertext", ciphertextOf(source.id().value()))
                .setParameter("id", target.id().value())
                .executeUpdate();
        flushAndClear();

        assertThatThrownBy(() -> repository.findById(target.id()))
                .isInstanceOf(CredentialDecryptionException.class)
                .hasMessageContaining(target.id().toString())
                .hasMessageNotContaining("pw");
    }

    @Test
    void rejectsADuplicateNameEvenWithoutCheckingFirst() {
        repository.save(aConnection("Local Kafka", BrokerType.KAFKA, "a:9092"));

        assertThatThrownBy(() -> repository.save(aConnection("Local Kafka", BrokerType.KAFKA, "b:9092")))
                .isInstanceOf(ConnectionNameTakenException.class);
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertThat(repository.findById(ConnectionId.newId())).isEmpty();
    }

    @Test
    void findsAllConnectionsOrderedByName() {
        repository.save(aConnection("RabbitMQ", BrokerType.RABBITMQ, "amqp://localhost:5672"));
        repository.save(aConnection("Kafka", BrokerType.KAFKA, "localhost:9092"));
        repository.save(aConnection("Pulsar", BrokerType.PULSAR, "pulsar://localhost:6650"));
        flushAndClear();

        var all = repository.findAll();

        assertThat(all).extracting(c -> c.name().value()).containsExactly("Kafka", "Pulsar", "RabbitMQ");
    }

    @Test
    void tellsWhetherANameIsTaken() {
        repository.save(aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092"));
        flushAndClear();

        assertThat(repository.existsByName(new ConnectionName("Local Kafka"))).isTrue();
        assertThat(repository.existsByName(new ConnectionName("Other"))).isFalse();
    }

    @Test
    void deletesAConnection() {
        var connection = aConnection("Local Kafka", BrokerType.KAFKA, "localhost:9092");
        repository.save(connection);
        flushAndClear();

        repository.deleteById(connection.id());
        flushAndClear();

        assertThat(repository.findById(connection.id())).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private byte[] ciphertextOf(UUID id) {
        return (byte[]) entityManager
                .createNativeQuery("SELECT credentials_ciphertext FROM broker_connection WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();
    }

    private static Connection aConnection(String name, BrokerType brokerType, String address) {
        return Connection.create(
                new ConnectionName(name), brokerType, new BrokerAddress(address), Credentials.none(), CREATED_AT);
    }
}
