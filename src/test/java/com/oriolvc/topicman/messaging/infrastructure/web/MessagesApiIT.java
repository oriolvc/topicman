package com.oriolvc.topicman.messaging.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.connection.application.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(properties = "topicman.kafka.send-timeout=2s")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
@TestConstructor(autowireMode = AutowireMode.ALL)
class MessagesApiIT {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final CreateConnectionUseCase createConnection;

    MessagesApiIT(MockMvcTester mvc, JdbcTemplate jdbc, CreateConnectionUseCase createConnection) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.createConnection = createConnection;
    }

    @BeforeEach
    void deleteAllConnections() {
        jdbc.update("DELETE FROM broker_connection");
    }

    @Test
    void producesEveryCopyToKafka() {
        var connection = aConnection(BrokerType.KAFKA, KAFKA.getBootstrapServers());
        var topic = "orders-" + UUID.randomUUID();

        var result = produce("""
                {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "%s"},
                 "key": "order-42", "headers": {"source": "topicman"},
                 "payload": {"format": "JSON", "content": "{\\"id\\": 42}"}, "copies": 3}
                """.formatted(connection, topic));

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.receipts")
                .asArray()
                .containsExactly(topic + "-0@0", topic + "-0@1", topic + "-0@2");
    }

    @Test
    void producesOneCopyByDefault() {
        var connection = aConnection(BrokerType.KAFKA, KAFKA.getBootstrapServers());

        var result = produce(aTextMessageTo(connection, "{\"type\": \"TOPIC\", \"name\": \"orders\"}"));

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.receipts")
                .asArray()
                .hasSize(1);
    }

    @Test
    void rejectsAJsonPayloadThatIsNotWellFormed() {
        var connection = aConnection(BrokerType.KAFKA, KAFKA.getBootstrapServers());

        var result = produce("""
                {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "orders"},
                 "payload": {"format": "JSON", "content": "{\\"id\\": "}}
                """.formatted(connection));

        assertProblem(result, HttpStatus.BAD_REQUEST, "messaging.invalid-message");
    }

    @Test
    void rejectsADestinationTheBrokerDoesNotSupport() {
        var connection = aConnection(BrokerType.KAFKA, KAFKA.getBootstrapServers());

        var result = produce(
                aTextMessageTo(connection, "{\"type\": \"EXCHANGE\", \"name\": \"orders\", \"routingKey\": \"\"}"));

        assertProblem(result, HttpStatus.BAD_REQUEST, "messaging.invalid-destination");
    }

    @Test
    void rejectsAKeyForABrokerWithoutMessageKeys() {
        var connection = aConnection(BrokerType.RABBITMQ, "amqp://localhost:5672");

        var result = produce("""
                {"connectionId": "%s", "destination": {"type": "EXCHANGE", "name": "orders", "routingKey": ""},
                 "key": "order-42", "payload": {"format": "TEXT", "content": "hi"}}
                """.formatted(connection));

        assertProblem(result, HttpStatus.BAD_REQUEST, "messaging.invalid-message");
    }

    @Test
    void rejectsARequestThatBreaksTheSchema() {
        var connection = aConnection(BrokerType.KAFKA, KAFKA.getBootstrapServers());

        var result = produce("""
                {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "orders"},
                 "payload": {"format": "TEXT", "content": "hi"}, "copies": 0}
                """.formatted(connection));

        assertProblem(result, HttpStatus.BAD_REQUEST, "messaging.invalid-request");
    }

    @Test
    void answersNotFoundForAnUnknownConnection() {
        var result = produce(aTextMessageTo(UUID.randomUUID(), "{\"type\": \"TOPIC\", \"name\": \"orders\"}"));

        assertProblem(result, HttpStatus.NOT_FOUND, "connection.not-found");
    }

    @Test
    void answersServiceUnavailableWhenTheBrokerCannotBeReached() {
        var connection = aConnection(BrokerType.KAFKA, "localhost:1");

        var result = produce(aTextMessageTo(connection, "{\"type\": \"TOPIC\", \"name\": \"orders\"}"));

        assertProblem(result, HttpStatus.SERVICE_UNAVAILABLE, "messaging.produce-failed");
        assertThat(result).bodyJson().extractingPath("$.sent").isEqualTo(0);
    }

    private UUID aConnection(BrokerType brokerType, String address) {
        return createConnection
                .execute(new CreateConnectionCommand(
                        new ConnectionName(brokerType + " " + UUID.randomUUID()),
                        brokerType,
                        new BrokerAddress(address),
                        Credentials.none()))
                .id();
    }

    private MvcTestResult produce(String json) {
        return mvc.post()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static String aTextMessageTo(UUID connection, String destination) {
        return """
                {"connectionId": "%s", "destination": %s, "payload": {"format": "TEXT", "content": "hi"}}
                """.formatted(connection, destination);
    }

    private static void assertProblem(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"status\": %d, \"code\": \"%s\"}".formatted(status.value(), code));
    }
}
