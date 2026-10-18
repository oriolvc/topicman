package com.oriolvc.topicman.connection.application;

import static com.oriolvc.topicman.connection.application.ConnectionFixtures.NOW;
import static com.oriolvc.topicman.connection.application.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.InvalidConnectionException;
import com.oriolvc.topicman.connection.domain.Secret;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UpdateConnectionUseCaseTest {

    private static final Clock LATER = Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC);

    private final InMemoryConnectionRepository repository = new InMemoryConnectionRepository();
    private final CreateConnectionUseCase create = new CreateConnectionUseCase(repository, ConnectionFixtures.CLOCK);
    private final UpdateConnectionUseCase useCase = new UpdateConnectionUseCase(repository, LATER);

    @Test
    void changesOnlyTheGivenProperties() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        var result =
                useCase.execute(new UpdateConnectionCommand(idOf(created), new ConnectionName("Staging"), null, null));

        assertThat(result.name()).isEqualTo("Staging");
        assertThat(result.address()).isEqualTo(created.address());
        assertThat(result.credentialsType()).isEqualTo(CredentialsType.NONE);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void changesAddressAndCredentials() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        var result = useCase.execute(new UpdateConnectionCommand(
                idOf(created),
                null,
                new BrokerAddress("kafka:9092"),
                Credentials.usernamePassword("app", new Secret("pw"))));

        assertThat(result.name()).isEqualTo("Local");
        assertThat(result.address()).isEqualTo("kafka:9092");
        assertThat(result.credentialsType()).isEqualTo(CredentialsType.USERNAME_PASSWORD);
        assertThat(repository.findById(idOf(created)))
                .hasValueSatisfying(stored -> assertThat(stored.credentials())
                        .isEqualTo(Credentials.usernamePassword("app", new Secret("pw"))));
    }

    @Test
    void keepsTheNameWhenRenamingToTheSameName() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        var result =
                useCase.execute(new UpdateConnectionCommand(idOf(created), new ConnectionName("Local"), null, null));

        assertThat(result.name()).isEqualTo("Local");
    }

    @Test
    void rejectsANameAnotherConnectionHas() {
        create.execute(aCreateKafkaConnectionCommand("Staging"));
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        assertThatThrownBy(() -> useCase.execute(
                        new UpdateConnectionCommand(idOf(created), new ConnectionName("Staging"), null, null)))
                .isInstanceOf(ConnectionNameTakenException.class);
    }

    @Test
    void rejectsCredentialsTheBrokerDoesNotSupportWithoutChangingAnything() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        assertThatThrownBy(() -> useCase.execute(new UpdateConnectionCommand(
                        idOf(created), new ConnectionName("Renamed"), null, Credentials.token(new Secret("jwt")))))
                .isInstanceOf(InvalidConnectionException.class);
        assertThat(repository.findById(idOf(created)))
                .hasValueSatisfying(stored -> assertThat(stored.name()).isEqualTo(new ConnectionName("Local")));
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(
                        new UpdateConnectionCommand(ConnectionId.newId(), new ConnectionName("x"), null, null)))
                .isInstanceOf(ConnectionNotFoundException.class);
    }

    private static ConnectionId idOf(ConnectionResult result) {
        return new ConnectionId(result.id());
    }
}
