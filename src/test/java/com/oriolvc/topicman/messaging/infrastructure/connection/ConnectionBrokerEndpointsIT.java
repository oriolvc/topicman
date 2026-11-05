package com.oriolvc.topicman.messaging.infrastructure.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.usecase.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TlsSettings;
import com.oriolvc.topicman.connection.domain.model.TrustedCertificates;
import com.oriolvc.topicman.messaging.domain.model.Authentication;
import com.oriolvc.topicman.messaging.domain.model.Tls;
import com.oriolvc.topicman.messaging.domain.port.BrokerEndpoints;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

/** A stored connection reaches the adapters with its decrypted credentials and TLS settings (ADR 0008). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class ConnectionBrokerEndpointsIT {

    private final CreateConnectionUseCase createConnection;
    private final BrokerEndpoints endpoints;

    ConnectionBrokerEndpointsIT(CreateConnectionUseCase createConnection, BrokerEndpoints endpoints) {
        this.createConnection = createConnection;
        this.endpoints = endpoints;
    }

    @Test
    void givesTheAdapterTheStoredCredentialsAndTlsSettings() {
        var created = createConnection.execute(new CreateConnectionCommand(
                new ConnectionName("Secure Kafka " + UUID.randomUUID()),
                BrokerType.KAFKA,
                new BrokerAddress("kafka.example.com:9093"),
                Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_512),
                new TlsSettings(true, new TrustedCertificates(TestCertificates.CA))));

        var endpoint = endpoints.of(new ConnectionId(created.id()));

        assertThat(endpoint.type()).isEqualTo(BrokerType.KAFKA);
        assertThat(endpoint.address()).isEqualTo("kafka.example.com:9093");
        assertThat(endpoint.authentication())
                .isEqualTo(new Authentication.UsernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_512));
        assertThat(endpoint.tls()).isEqualTo(new Tls(true, TestCertificates.CA.strip()));
    }

    @Test
    void givesThePulsarToken() {
        var created = createConnection.execute(new CreateConnectionCommand(
                new ConnectionName("Secure Pulsar " + UUID.randomUUID()),
                BrokerType.PULSAR,
                new BrokerAddress("pulsar+ssl://pulsar.example.com:6651"),
                Credentials.token(new Secret("jwt"))));

        var endpoint = endpoints.of(new ConnectionId(created.id()));

        assertThat(endpoint.authentication()).isEqualTo(new Authentication.Token(new Secret("jwt")));
        assertThat(endpoint.tls()).isEqualTo(Tls.defaults());
    }
}
