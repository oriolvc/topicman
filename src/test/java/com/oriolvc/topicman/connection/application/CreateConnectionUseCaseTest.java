package com.oriolvc.topicman.connection.application;

import static com.oriolvc.topicman.connection.application.ConnectionFixtures.CLOCK;
import static com.oriolvc.topicman.connection.application.ConnectionFixtures.NOW;
import static com.oriolvc.topicman.connection.application.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.Secret;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
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
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(repository.findById(new ConnectionId(result.id())))
                .hasValueSatisfying(stored -> assertThat(stored.credentials())
                        .isEqualTo(Credentials.usernamePassword("app", new Secret("pw"))));
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
