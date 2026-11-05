package com.oriolvc.topicman.connection.application.usecase;

import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.NOW;
import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.connection.application.model.ConnectionResult;
import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.model.CredentialsType;
import com.oriolvc.topicman.connection.application.model.UpdateConnectionCommand;
import com.oriolvc.topicman.connection.domain.exception.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TlsSettings;
import com.oriolvc.topicman.connection.domain.model.TrustedCertificates;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
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
                        .isEqualTo(Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.PLAIN)));
    }

    @Test
    void changesTlsSettings() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));
        var certificates = new TrustedCertificates(TestCertificates.CA);

        var result = useCase.execute(
                new UpdateConnectionCommand(idOf(created), null, null, null, new TlsSettings(true, certificates)));

        assertThat(result.tlsEnabled()).isTrue();
        assertThat(result.trustedCertificates()).isEqualTo(certificates.pem());
        assertThat(result.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void keepsTlsSettingsWhenNoneAreGiven() {
        var created = create.execute(new CreateConnectionCommand(
                new ConnectionName("Local"),
                BrokerType.KAFKA,
                new BrokerAddress("localhost:9093"),
                Credentials.none(),
                new TlsSettings(true, null)));

        var result =
                useCase.execute(new UpdateConnectionCommand(idOf(created), new ConnectionName("Renamed"), null, null));

        assertThat(result.tlsEnabled()).isTrue();
    }

    @Test
    void rejectsTlsSettingsTheBrokerDoesNotSupportWithoutChangingAnything() {
        var created = create.execute(new CreateConnectionCommand(
                new ConnectionName("Local Pulsar"),
                BrokerType.PULSAR,
                new BrokerAddress("pulsar://localhost:6650"),
                Credentials.none()));

        assertThatThrownBy(() -> useCase.execute(new UpdateConnectionCommand(
                        idOf(created),
                        new ConnectionName("Renamed"),
                        new BrokerAddress("pulsar+ssl://localhost:6651"),
                        null,
                        new TlsSettings(true, null))))
                .isInstanceOf(InvalidConnectionException.class);
        assertThat(repository.findById(idOf(created))).hasValueSatisfying(stored -> {
            assertThat(stored.name()).isEqualTo(new ConnectionName("Local Pulsar"));
            assertThat(stored.address()).isEqualTo(new BrokerAddress("pulsar://localhost:6650"));
        });
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
                        new UpdateConnectionCommand(TestConnectionIds.next(), new ConnectionName("x"), null, null)))
                .isInstanceOf(ConnectionNotFoundException.class);
    }

    private static ConnectionId idOf(ConnectionResult result) {
        return new ConnectionId(result.id());
    }
}
