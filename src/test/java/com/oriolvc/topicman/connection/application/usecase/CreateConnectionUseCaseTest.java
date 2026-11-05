package com.oriolvc.topicman.connection.application.usecase;

import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.CLOCK;
import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.NOW;
import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.model.CredentialsType;
import com.oriolvc.topicman.connection.domain.exception.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TlsSettings;
import com.oriolvc.topicman.connection.domain.model.TrustedCertificates;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import org.junit.jupiter.api.Test;

class CreateConnectionUseCaseTest {

    private final InMemoryConnectionRepository repository = new InMemoryConnectionRepository();
    private final CreateConnectionUseCase useCase = new CreateConnectionUseCase(repository, CLOCK);

    @Test
    void createsAndStoresAConnection() {
        var command = new CreateConnectionCommand(
                new ConnectionName("Local Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("localhost:9092"),
                Credentials.usernamePassword("app", new Secret("pw")));

        var result = useCase.execute(command);

        assertThat(result.name()).isEqualTo("Local Kafka");
        assertThat(result.brokerType()).isEqualTo(BrokerType.KAFKA);
        assertThat(result.address()).isEqualTo("localhost:9092");
        assertThat(result.credentialsType()).isEqualTo(CredentialsType.USERNAME_PASSWORD);
        assertThat(result.saslMechanism()).isEqualTo(SaslMechanism.PLAIN);
        assertThat(result.tlsEnabled()).isFalse();
        assertThat(result.trustedCertificates()).isNull();
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(repository.findById(new ConnectionId(result.id())))
                .hasValueSatisfying(stored -> assertThat(stored.credentials())
                        .isEqualTo(Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.PLAIN)));
    }

    @Test
    void createsAConnectionWithTlsSettings() {
        var certificates = new TrustedCertificates(TestCertificates.CA);
        var command = new CreateConnectionCommand(
                new ConnectionName("Secure Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("kafka.example.com:9093"),
                Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_512),
                new TlsSettings(true, certificates));

        var result = useCase.execute(command);

        assertThat(result.saslMechanism()).isEqualTo(SaslMechanism.SCRAM_SHA_512);
        assertThat(result.tlsEnabled()).isTrue();
        assertThat(result.trustedCertificates()).isEqualTo(certificates.pem());
    }

    @Test
    void hasNoSaslMechanismForCredentialsWithoutOne() {
        var command = new CreateConnectionCommand(
                new ConnectionName("Local RabbitMQ"),
                BrokerType.RABBITMQ,
                new BrokerAddress("amqp://localhost:5672"),
                Credentials.usernamePassword("guest", new Secret("guest")));

        var result = useCase.execute(command);

        assertThat(result.saslMechanism()).isNull();
    }

    @Test
    void rejectsANameAnotherConnectionHas() {
        useCase.execute(aCreateKafkaConnectionCommand("Local Kafka"));

        assertThatThrownBy(() -> useCase.execute(aCreateKafkaConnectionCommand("Local Kafka")))
                .isInstanceOf(ConnectionNameTakenException.class)
                .hasFieldOrPropertyWithValue("code", "connection.name-taken");
        assertThat(repository.findAll()).hasSize(1);
    }
}
