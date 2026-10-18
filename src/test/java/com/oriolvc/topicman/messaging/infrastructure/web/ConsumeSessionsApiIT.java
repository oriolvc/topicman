package com.oriolvc.topicman.messaging.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.connection.application.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/** Over real HTTP, because what matters is what an EventSource client receives on the wire. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"topicman.kafka.send-timeout=3s", "topicman.kafka.open-timeout=3s"})
@Import(TestcontainersConfiguration.class)
@Testcontainers
@TestConstructor(autowireMode = AutowireMode.ALL)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ConsumeSessionsApiIT {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<EventStream> openStreams = new CopyOnWriteArrayList<>();
    private final JdbcTemplate jdbc;
    private final CreateConnectionUseCase createConnection;

    @LocalServerPort
    private int port;

    ConsumeSessionsApiIT(JdbcTemplate jdbc, CreateConnectionUseCase createConnection) {
        this.jdbc = jdbc;
        this.createConnection = createConnection;
    }

    @BeforeEach
    void deleteAllConnections() {
        jdbc.update("DELETE FROM broker_connection");
    }

    @AfterEach
    void disconnectStreams() {
        openStreams.forEach(EventStream::disconnect);
    }

    @Test
    void opensASession() throws Exception {
        var connection = aKafkaConnection();
        var topic = anExistingTopic(connection);

        var response = post("/api/v1/consume-sessions", openRequest(connection, topic, "EARLIEST"));

        assertThat(response.statusCode()).isEqualTo(201);
        String id = JsonPath.read(response.body(), "$.id");
        assertThat(response.headers().firstValue("Location")).hasValue("/api/v1/consume-sessions/" + id);
        assertThat(JsonPath.<String>read(response.body(), "$.eventsUrl"))
                .isEqualTo("/api/v1/consume-sessions/" + id + "/events");
        assertThat(JsonPath.<String>read(response.body(), "$.destination.name")).isEqualTo(topic);
        assertThat(JsonPath.<String>read(response.body(), "$.startPosition")).isEqualTo("EARLIEST");
    }

    @Test
    void streamsMessagesProducedAfterOpeningAsServerSentEvents() throws Exception {
        var connection = aKafkaConnection();
        var topic = anExistingTopic(connection);
        var events = connect(eventsUrlOf(post("/api/v1/consume-sessions", openRequest(connection, topic, "LATEST"))));

        produce(connection, topic, "{\"format\": \"TEXT\", \"content\": \"{\\\"id\\\": 42}\"}");

        events.awaitLine("event:message");
        events.awaitLine("id:" + topic + "-0@1");
        var data = events.awaitLineStartingWith("data:").substring("data:".length());
        assertThat(JsonPath.<String>read(data, "$.payload.format")).isEqualTo("JSON");
        assertThat(JsonPath.<String>read(data, "$.payload.content")).isEqualTo("{\"id\": 42}");
        assertThat(JsonPath.<String>read(data, "$.position")).isEqualTo(topic + "-0@1");
        assertThat(events.lines()).noneMatch(line -> line.contains("\"before\""));
    }

    @Test
    void endsTheStreamWithAClosedEventWhenTheSessionIsClosed() throws Exception {
        var connection = aKafkaConnection();
        var session = post("/api/v1/consume-sessions", openRequest(connection, anExistingTopic(connection), "LATEST"));
        var events = connect(eventsUrlOf(session));

        var deleted = delete("/api/v1/consume-sessions/" + JsonPath.read(session.body(), "$.id"));

        assertThat(deleted.statusCode()).isEqualTo(204);
        events.awaitLine("event:closed");
        events.awaitEnd();
    }

    @Test
    void rejectsASecondClientWhileOneIsStreaming() throws Exception {
        var connection = aKafkaConnection();
        var session = post("/api/v1/consume-sessions", openRequest(connection, anExistingTopic(connection), "LATEST"));
        connect(eventsUrlOf(session));

        var second = getEvents(eventsUrlOf(session));

        assertProblem(second, 409, "messaging.session-in-use");
    }

    @Test
    void answersNotFoundForTheEventsOfAnUnknownSession() throws Exception {
        var response = getEvents("/api/v1/consume-sessions/" + UUID.randomUUID() + "/events");

        assertProblem(response, 404, "messaging.session-not-found");
    }

    @Test
    void cannotCloseASessionTwice() throws Exception {
        var connection = aKafkaConnection();
        var session = post("/api/v1/consume-sessions", openRequest(connection, anExistingTopic(connection), "LATEST"));
        var url = "/api/v1/consume-sessions/" + JsonPath.read(session.body(), "$.id");
        delete(url);

        assertProblem(delete(url), 404, "messaging.session-not-found");
        assertProblem(getEvents(url + "/events"), 404, "messaging.session-not-found");
    }

    @Test
    void answersServiceUnavailableForAMissingTopic() throws Exception {
        var connection = aKafkaConnection();

        var response =
                post("/api/v1/consume-sessions", openRequest(connection, "missing-" + UUID.randomUUID(), "LATEST"));

        assertProblem(response, 503, "messaging.read-failed");
    }

    @Test
    void rejectsADestinationTheBrokerDoesNotSupport() throws Exception {
        var response = post("/api/v1/consume-sessions", """
                {"connectionId": "%s", "destination": {"type": "EXCHANGE", "name": "orders", "routingKey": ""}}
                """.formatted(aKafkaConnection()));

        assertProblem(response, 400, "messaging.invalid-destination");
    }

    @Test
    void answersNotFoundForAnUnknownConnection() throws Exception {
        var response = post("/api/v1/consume-sessions", openRequest(UUID.randomUUID(), "orders", "LATEST"));

        assertProblem(response, 404, "connection.not-found");
    }

    private UUID aKafkaConnection() {
        return createConnection
                .execute(new CreateConnectionCommand(
                        new ConnectionName("Kafka " + UUID.randomUUID()),
                        BrokerType.KAFKA,
                        new BrokerAddress(KAFKA.getBootstrapServers()),
                        Credentials.none()))
                .id();
    }

    /** Producing creates the topic; its first message is at offset 0. */
    private String anExistingTopic(UUID connection) throws Exception {
        var topic = "orders-" + UUID.randomUUID();
        produce(connection, topic, "{\"format\": \"TEXT\", \"content\": \"before\"}");
        return topic;
    }

    private void produce(UUID connection, String topic, String payload) throws Exception {
        var response = post("/api/v1/messages", """
                {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "%s"}, "payload": %s}
                """.formatted(connection, topic, payload));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private static String openRequest(UUID connection, String topic, String startPosition) {
        return """
                {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "%s"}, "startPosition": "%s"}
                """.formatted(connection, topic, startPosition);
    }

    private static String eventsUrlOf(HttpResponse<String> session) {
        assertThat(session.statusCode()).as(session.body()).isEqualTo(201);
        return JsonPath.read(session.body(), "$.eventsUrl");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getEvents(String path) throws Exception {
        return http.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Accept", "text/event-stream")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private EventStream connect(String path) throws Exception {
        var response = http.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Accept", "text/event-stream")
                        .build(),
                HttpResponse.BodyHandlers.ofLines());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(type -> assertThat(type).startsWith("text/event-stream"));
        var stream = new EventStream(response.body());
        openStreams.add(stream);
        return stream;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo(code);
    }

    /** Collects the lines of an event stream on a virtual thread. */
    private static final class EventStream {

        private final Stream<String> body;
        private final List<String> lines = new CopyOnWriteArrayList<>();
        private final Thread reader;

        EventStream(Stream<String> body) {
            this.body = body;
            this.reader = Thread.startVirtualThread(() -> {
                try {
                    body.forEach(lines::add);
                } catch (RuntimeException ignored) {
                    // The test disconnected.
                }
            });
        }

        List<String> lines() {
            return List.copyOf(lines);
        }

        void awaitLine(String line) {
            await().atMost(Duration.ofSeconds(15)).until(() -> lines.contains(line));
        }

        String awaitLineStartingWith(String prefix) {
            await().atMost(Duration.ofSeconds(15))
                    .until(() -> lines.stream().anyMatch(line -> line.startsWith(prefix)));
            return lines.stream()
                    .filter(line -> line.startsWith(prefix))
                    .findFirst()
                    .orElseThrow();
        }

        void awaitEnd() {
            await().atMost(Duration.ofSeconds(15)).until(() -> !reader.isAlive());
        }

        void disconnect() {
            body.close();
        }
    }
}
