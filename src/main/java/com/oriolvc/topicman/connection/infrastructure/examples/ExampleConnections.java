package com.oriolvc.topicman.connection.infrastructure.examples;

import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.model.ListConnectionsCommand;
import com.oriolvc.topicman.connection.application.usecase.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.application.usecase.ListConnectionsUseCase;
import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TlsSettings;
import com.oriolvc.topicman.connection.domain.model.TrustedCertificates;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import com.oriolvc.topicman.shared.infrastructure.examples.ExampleData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * One connection per kind of credentials, for the brokers in {@code compose.demo.yaml}. Its
 * credentials and CA certificate are public and for that demo only, so these connections reach
 * nothing else, and fail their connection test unless the demo brokers run on this machine.
 */
@Component
class ExampleConnections implements ExampleData {

    private static final String USERNAME = "demo";
    private static final Secret PASSWORD = new Secret("demo-secret");
    private static final Secret TOKEN = new Secret(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vIn0.Z_RlpKTzK4icbhYrY7W_fURYnn48EFPhfgh02uo4wx8");
    private static final String DEMO_CA = "examples/demo-ca.pem";

    private final ListConnectionsUseCase list;
    private final CreateConnectionUseCase create;

    ExampleConnections(ListConnectionsUseCase list, CreateConnectionUseCase create) {
        this.list = list;
        this.create = create;
    }

    @Override
    public boolean isEmpty() {
        return list.execute(new ListConnectionsCommand()).isEmpty();
    }

    @Override
    public void load() {
        var kafkaTls = new TlsSettings(true, demoCa());
        // Pulsar and RabbitMQ switch TLS on with the address scheme, and only need the certificate.
        var addressTls = new TlsSettings(false, demoCa());
        add("Kafka demo", BrokerType.KAFKA, "localhost:9092", Credentials.none(), TlsSettings.disabled());
        add("Kafka demo, SASL PLAIN and TLS", BrokerType.KAFKA, "localhost:9094", kafka(SaslMechanism.PLAIN), kafkaTls);
        add(
                "Kafka demo, SCRAM-SHA-256 and TLS",
                BrokerType.KAFKA,
                "localhost:9094",
                kafka(SaslMechanism.SCRAM_SHA_256),
                kafkaTls);
        add(
                "Kafka demo, SCRAM-SHA-512 and TLS",
                BrokerType.KAFKA,
                "localhost:9094",
                kafka(SaslMechanism.SCRAM_SHA_512),
                kafkaTls);
        add("Pulsar demo", BrokerType.PULSAR, "pulsar://localhost:6650", Credentials.none(), TlsSettings.disabled());
        add(
                "Pulsar demo, token and TLS",
                BrokerType.PULSAR,
                "pulsar+ssl://localhost:6651",
                Credentials.token(TOKEN),
                addressTls);
        add("RabbitMQ demo", BrokerType.RABBITMQ, "amqp://localhost:5672", Credentials.none(), TlsSettings.disabled());
        add(
                "RabbitMQ demo, user and TLS",
                BrokerType.RABBITMQ,
                "amqps://localhost:5671",
                Credentials.usernamePassword(USERNAME, PASSWORD),
                addressTls);
    }

    private static Credentials kafka(SaslMechanism mechanism) {
        return Credentials.usernamePassword(USERNAME, PASSWORD, mechanism);
    }

    private void add(String name, BrokerType type, String address, Credentials credentials, TlsSettings tls) {
        create.execute(new CreateConnectionCommand(
                new ConnectionName(name), type, new BrokerAddress(address), credentials, tls));
    }

    private static TrustedCertificates demoCa() {
        try {
            return new TrustedCertificates(new ClassPathResource(DEMO_CA).getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
