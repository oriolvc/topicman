package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.messaging.infrastructure.broker.BrokerAdapterContractTest;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.pulsar.PulsarContainer;

@Testcontainers
class PulsarAdapterContractIT extends BrokerAdapterContractTest {

    @Container
    private static final PulsarContainer PULSAR = new PulsarContainer("apachepulsar/pulsar:4.2.4");

    private static final PulsarProperties PROPERTIES =
            new PulsarProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(3));

    private static final String NAMESPACE_PATH = "/admin/v2/persistent/public/default";

    // The admin REST API through a plain HTTP client, so the tests need no Pulsar admin library.
    private static HttpClient http;

    // Clients are shared by every producer and reader, as in the application.
    private static PulsarClients clients;

    @BeforeAll
    static void connect() {
        http = HttpClient.newHttpClient();
        clients = new PulsarClients(PROPERTIES);
    }

    @AfterAll
    static void disconnect() {
        clients.close();
        http.close();
    }

    @Override
    protected BrokerType brokerType() {
        return BrokerType.PULSAR;
    }

    @Override
    protected MessageProducer createProducer() {
        return new PulsarMessageProducer(clients, PROPERTIES);
    }

    @Override
    protected MessageReader createReader() {
        return new PulsarMessageReader(clients, PROPERTIES);
    }

    @Override
    protected BrokerEndpoint endpoint() {
        return new BrokerEndpoint(BrokerType.PULSAR, PULSAR.getPulsarBrokerUrl());
    }

    @Override
    protected BrokerEndpoint unreachableEndpoint() {
        return new BrokerEndpoint(BrokerType.PULSAR, "pulsar://localhost:1");
    }

    @Override
    protected Destination newDestination() {
        var topic = "topic-" + UUID.randomUUID();
        admin("PUT", "/" + topic, "");
        return Destination.topic(topic);
    }

    @Override
    protected Destination missingDestination() {
        return Destination.topic("missing-" + UUID.randomUUID());
    }

    @Override
    protected Destination unsupportedDestination() {
        return Destination.exchange("orders", "created");
    }

    @Override
    protected void assertNoReaderStateLeftFor(Destination destination) {
        var topic = ((Destination.Topic) destination).name();
        assertThat(admin("GET", "/" + topic + "/subscriptions", null)).isEqualTo("[]");
    }

    @Test
    void usesTheMessageIdAsReceiptAndPosition() {
        var destination = newDestination();

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            var receipt = producer.send(endpoint(), destination, Message.of(Payload.text("hello")));

            assertThat(pollUntil(stream, 1).getFirst().position()).isEqualTo(receipt.reference());
        }
    }

    @Test
    void readsFromEveryPartitionOfAPartitionedTopic() {
        var topic = "partitioned-" + UUID.randomUUID();
        admin("PUT", "/" + topic + "/partitions", "3");
        var destination = Destination.topic(topic);

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            for (int i = 0; i < 6; i++) {
                producer.send(
                        endpoint(),
                        destination,
                        Message.of(Payload.text("m" + i)).withKey("key-" + i));
            }

            assertThat(payloadsOf(pollUntil(stream, 6))).containsExactlyInAnyOrder("m0", "m1", "m2", "m3", "m4", "m5");
        }
    }

    @Test
    void acceptsAFullyQualifiedTopicName() {
        var topic = "topic-" + UUID.randomUUID();
        admin("PUT", "/" + topic, "");
        var destination = Destination.topic("persistent://public/default/" + topic);

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            producer.send(endpoint(), destination, Message.of(Payload.text("qualified")));

            assertThat(payloadsOf(pollUntil(stream, 1))).containsExactly("qualified");
        }
    }

    @Test
    void doesNotCreateAMissingTopicWhenReadingIt() {
        var topic = "missing-" + UUID.randomUUID();

        assertThatThrownBy(() -> reader.open(endpoint(), Destination.topic(topic), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageContaining(topic);
        assertThat(admin("GET", "", null)).doesNotContain(topic);
    }

    @Test
    void failsOnAnInvalidAddress() {
        var invalid = new BrokerEndpoint(BrokerType.PULSAR, "not-a-service-url");

        assertThatThrownBy(() -> producer.send(invalid, Destination.topic("orders"), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);
        assertThatThrownBy(() -> reader.open(invalid, Destination.topic("orders"), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);
    }

    @Test
    void namesTheServiceUrlItCannotReach() {
        var endpoint = new BrokerEndpoint(BrokerType.PULSAR, "pulsar://localhost:1,localhost:2");

        assertThatThrownBy(() -> producer.send(endpoint, newDestination(), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class)
                .hasMessageStartingWith("Cannot reach the Pulsar broker at pulsar://localhost:1,localhost:2: ");
        assertThatThrownBy(() -> reader.open(endpoint, newDestination(), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageStartingWith("Cannot reach the Pulsar broker at pulsar://localhost:1,localhost:2: ");
    }

    @Test
    void producesAgainAfterAFailedSend() {
        var destination = newDestination();
        assertThatThrownBy(() -> producer.send(unreachableEndpoint(), destination, Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);

        var receipt = producer.send(endpoint(), destination, Message.of(Payload.text("ok")));

        assertThat(receipt.reference()).isNotBlank();
    }

    private static String admin(String method, String path, String body) {
        var request = HttpRequest.newBuilder(URI.create(PULSAR.getHttpServiceUrl() + NAMESPACE_PATH + path))
                .header("Content-Type", "application/json")
                .method(
                        method,
                        body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        method + " " + path + " returned " + response.statusCode() + ": " + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException(method + " " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(method + " " + path + " interrupted", e);
        }
    }
}
