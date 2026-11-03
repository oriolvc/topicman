package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.messaging.infrastructure.broker.BrokerAdapterContractTest;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@Testcontainers
class RabbitMqAdapterContractIT extends BrokerAdapterContractTest {

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.2-management");

    private static final RabbitMqProperties PROPERTIES =
            new RabbitMqProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(3));

    private static final String ROUTING_KEY = "created";

    // Shared by every producer and reader, as in the application.
    private static RabbitMqConnections connections;

    // A plain client, to set up exchanges and queues the way real applications do.
    private static Connection admin;

    // The management REST API, to inspect bindings left on the broker.
    private static HttpClient http;

    @BeforeAll
    static void connect() throws Exception {
        connections = new RabbitMqConnections(PROPERTIES);
        var factory = new ConnectionFactory();
        factory.setUri(RABBITMQ.getAmqpUrl());
        admin = factory.newConnection();
        http = HttpClient.newHttpClient();
    }

    @AfterAll
    static void disconnect() throws Exception {
        http.close();
        admin.close();
        connections.close();
    }

    @Override
    protected BrokerType brokerType() {
        return BrokerType.RABBITMQ;
    }

    @Override
    protected MessageProducer createProducer() {
        return new RabbitMqMessageProducer(connections, PROPERTIES);
    }

    @Override
    protected MessageReader createReader() {
        return new RabbitMqMessageReader(connections);
    }

    @Override
    protected BrokerEndpoint endpoint() {
        return new BrokerEndpoint(BrokerType.RABBITMQ, RABBITMQ.getAmqpUrl());
    }

    @Override
    protected BrokerEndpoint unreachableEndpoint() {
        return new BrokerEndpoint(BrokerType.RABBITMQ, "amqp://localhost:1");
    }

    @Override
    protected Destination newDestination() {
        return Destination.exchange(declareExchange(BuiltinExchangeType.DIRECT), ROUTING_KEY);
    }

    @Override
    protected Destination missingDestination() {
        return Destination.exchange("missing-" + UUID.randomUUID(), ROUTING_KEY);
    }

    @Override
    protected Destination unsupportedDestination() {
        return Destination.topic("orders");
    }

    @Override
    protected boolean canReadFromBeginning() {
        return false;
    }

    @Override
    protected boolean supportsMessageKeys() {
        return false;
    }

    @Override
    protected void assertNoReaderStateLeftFor(Destination destination) {
        var exchange = ((Destination.Exchange) destination).name();
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(management("/api/exchanges/%2F/" + encode(exchange) + "/bindings/source"))
                                .isEqualTo("[]"));
    }

    @Test
    void neverTakesMessagesFromExistingQueues() throws Exception {
        var exchange = declareExchange(BuiltinExchangeType.DIRECT);
        var queue = "orders-" + UUID.randomUUID();
        try (var channel = admin.createChannel()) {
            channel.queueDeclare(queue, false, false, true, null);
            channel.queueBind(queue, exchange, ROUTING_KEY);
        }
        var destination = Destination.exchange(exchange, ROUTING_KEY);

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            producer.send(endpoint(), destination, Message.of(Payload.text("hello")));
            pollUntil(stream, 1);
        }

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(messageCount(queue)).isOne());
    }

    @Test
    void readsOnlyMessagesWithTheRoutingKey() {
        var exchange = declareExchange(BuiltinExchangeType.DIRECT);

        try (var stream = reader.open(endpoint(), Destination.exchange(exchange, "created"), StartPosition.LATEST)) {
            producer.send(endpoint(), Destination.exchange(exchange, "deleted"), Message.of(Payload.text("other")));
            producer.send(endpoint(), Destination.exchange(exchange, "created"), Message.of(Payload.text("wanted")));

            assertThat(payloadsOf(pollUntil(stream, 1))).containsExactly("wanted");
        }
    }

    @Test
    void readsFromAFanoutExchangeWithAnEmptyRoutingKey() {
        var destination = Destination.exchange(declareExchange(BuiltinExchangeType.FANOUT), "");

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            producer.send(endpoint(), destination, Message.of(Payload.text("broadcast")));

            assertThat(payloadsOf(pollUntil(stream, 1))).containsExactly("broadcast");
        }
    }

    @Test
    void setsTheContentTypeFromThePayloadFormat() throws Exception {
        var exchange = declareExchange(BuiltinExchangeType.DIRECT);
        var queue = "inspect-" + UUID.randomUUID();
        try (var channel = admin.createChannel()) {
            channel.queueDeclare(queue, false, false, true, null);
            channel.queueBind(queue, exchange, ROUTING_KEY);
            var destination = Destination.exchange(exchange, ROUTING_KEY);

            producer.send(endpoint(), destination, Message.of(Payload.json("{\"id\":1}")));
            producer.send(endpoint(), destination, Message.of(Payload.text("plain")));

            assertThat(nextMessage(channel, queue).getProps().getContentType()).isEqualTo("application/json");
            assertThat(nextMessage(channel, queue).getProps().getContentType()).isEqualTo("text/plain");
        }
    }

    @Test
    void rejectsAMessageKey() {
        var message = Message.of(Payload.text("x")).withKey("order-42");

        assertThatThrownBy(() -> producer.send(endpoint(), newDestination(), message))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("routing key");
    }

    @Test
    void failsToProduceToAMissingExchangeAndProducesAgainAfterwards() {
        var missing = missingDestination();

        assertThatThrownBy(() -> producer.send(endpoint(), missing, Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class)
                .hasMessageContaining(((Destination.Exchange) missing).name());
        assertThat(producer.send(endpoint(), newDestination(), Message.of(Payload.text("ok")))
                        .reference())
                .isNotBlank();
    }

    @Test
    void failsOnAnInvalidAddress() {
        var invalid = new BrokerEndpoint(BrokerType.RABBITMQ, "not a uri");

        assertThatThrownBy(() -> producer.send(invalid, newDestination(), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);
        assertThatThrownBy(() -> reader.open(invalid, newDestination(), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);
    }

    @Test
    void namesOnlyTheHostAndPortItCannotReach() {
        var endpoint = new BrokerEndpoint(BrokerType.RABBITMQ, "amqp://someone:secret@localhost:1/vhost");

        assertThatThrownBy(() -> producer.send(endpoint, newDestination(), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class)
                .hasMessageStartingWith("Cannot reach the RabbitMQ broker at localhost:1")
                .hasMessageNotContaining("secret");
        assertThatThrownBy(() -> reader.open(endpoint, newDestination(), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageStartingWith("Cannot reach the RabbitMQ broker at localhost:1")
                .hasMessageNotContaining("secret");
    }

    private static String declareExchange(BuiltinExchangeType type) {
        var exchange = "exchange-" + UUID.randomUUID();
        try (var channel = admin.createChannel()) {
            channel.exchangeDeclare(exchange, type);
        } catch (Exception e) {
            throw new IllegalStateException("Could not declare exchange " + exchange, e);
        }
        return exchange;
    }

    private static long messageCount(String queue) throws Exception {
        try (var channel = admin.createChannel()) {
            return channel.queueDeclarePassive(queue).getMessageCount();
        }
    }

    private static com.rabbitmq.client.GetResponse nextMessage(Channel channel, String queue) {
        var response = new com.rabbitmq.client.GetResponse[1];
        await().atMost(Duration.ofSeconds(5)).until(() -> (response[0] = channel.basicGet(queue, true)) != null);
        return response[0];
    }

    private static String management(String path) {
        var credentials = RABBITMQ.getAdminUsername() + ":" + RABBITMQ.getAdminPassword();
        var request = HttpRequest.newBuilder(URI.create(RABBITMQ.getHttpUrl() + path))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(UTF_8)))
                .GET()
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("GET " + path + " returned " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("GET " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GET " + path + " interrupted", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }
}
