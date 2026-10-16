package com.oriolvc.topicman.connection.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@DataJpaTest
@TestConstructor(autowireMode = AutowireMode.ALL)
@Import({TestcontainersConfiguration.class, JpaConnectionRepository.class})
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

    private static Connection aConnection(String name, BrokerType brokerType, String address) {
        return Connection.create(new ConnectionName(name), brokerType, new BrokerAddress(address), CREATED_AT);
    }
}
