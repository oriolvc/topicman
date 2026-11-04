package com.oriolvc.topicman.connection.application;

import static com.oriolvc.topicman.connection.application.ConnectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.TlsSettings;
import com.oriolvc.topicman.connection.domain.TrustedCertificates;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import org.junit.jupiter.api.Test;

class GetBrokerAccessUseCaseTest {

    private final InMemoryConnectionRepository repository = new InMemoryConnectionRepository();
    private final CreateConnectionUseCase create = new CreateConnectionUseCase(repository, CLOCK);
    private final GetBrokerAccessUseCase useCase = new GetBrokerAccessUseCase(repository);

    @Test
    void givesKafkaItsUsernamePasswordMechanismAndTls() {
        var created = create.execute(new CreateConnectionCommand(
                new ConnectionName("Secure Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("kafka.example.com:9093"),
                Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_256),
                new TlsSettings(true, new TrustedCertificates(TestCertificates.CA))));

        var access = useCase.execute(new GetBrokerAccessCommand(new ConnectionId(created.id())));

        assertThat(access.brokerType()).isEqualTo(BrokerType.KAFKA);
        assertThat(access.address()).isEqualTo("kafka.example.com:9093");
        assertThat(access.credentials())
                .isEqualTo(
                        new BrokerCredentials.UsernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_256));
        assertThat(access.tlsEnabled()).isTrue();
        assertThat(access.trustedCertificates()).isEqualTo(TestCertificates.CA.strip());
    }

    @Test
    void givesPulsarItsToken() {
        var created = create.execute(new CreateConnectionCommand(
                new ConnectionName("Secure Pulsar"),
                BrokerType.PULSAR,
                new BrokerAddress("pulsar+ssl://pulsar.example.com:6651"),
                Credentials.token(new Secret("jwt"))));

        var access = useCase.execute(new GetBrokerAccessCommand(new ConnectionId(created.id())));

        assertThat(access.credentials()).isEqualTo(new BrokerCredentials.Token(new Secret("jwt")));
        assertThat(access.tlsEnabled()).isFalse();
        assertThat(access.trustedCertificates()).isNull();
    }

    @Test
    void givesNoCredentialsWhenTheConnectionHasNone() {
        var created = create.execute(ConnectionFixtures.aCreateKafkaConnectionCommand("Local"));

        var access = useCase.execute(new GetBrokerAccessCommand(new ConnectionId(created.id())));

        assertThat(access.credentials()).isEqualTo(BrokerCredentials.none());
    }

    @Test
    void neverShowsSecretsInToString() {
        var created = create.execute(new CreateConnectionCommand(
                new ConnectionName("Secure Kafka"),
                BrokerType.KAFKA,
                new BrokerAddress("kafka:9093"),
                Credentials.usernamePassword("app", new Secret("pw-value"))));

        var access = useCase.execute(new GetBrokerAccessCommand(new ConnectionId(created.id())));

        assertThat(access.toString()).doesNotContain("pw-value");
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(new GetBrokerAccessCommand(TestConnectionIds.next())))
                .isInstanceOf(ConnectionNotFoundException.class);
    }
}
