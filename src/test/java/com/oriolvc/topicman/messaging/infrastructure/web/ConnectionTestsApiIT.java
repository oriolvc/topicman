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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
@TestConstructor(autowireMode = AutowireMode.ALL)
class ConnectionTestsApiIT {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final CreateConnectionUseCase createConnection;

    ConnectionTestsApiIT(MockMvcTester mvc, JdbcTemplate jdbc, CreateConnectionUseCase createConnection) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.createConnection = createConnection;
    }

    @BeforeEach
    void deleteAllConnections() {
        jdbc.update("DELETE FROM broker_connection");
    }

    @Test
    void reportsAWorkingConnection() {
        var connection = aKafkaConnection(KAFKA.getBootstrapServers());

        var result = testConnection("{\"connectionId\": \"%s\"}".formatted(connection));

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("SUCCEEDED");
        assertThat(result).bodyJson().extractingPath("$.detail").asString().startsWith("Kafka cluster ");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.durationMs")
                .asNumber()
                .satisfies(duration -> assertThat(duration.longValue()).isNotNegative());
    }

    @Test
    void reportsAnUnreachableBrokerAsAFailedTestNotAnError() {
        var connection = aKafkaConnection("localhost:1");

        var result = testConnection("{\"connectionId\": \"%s\"}".formatted(connection));

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("FAILED");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.detail")
                .asString()
                .startsWith("Cannot reach the Kafka broker at localhost:1");
    }

    @Test
    void answersNotFoundForAnUnknownConnection() {
        var result = testConnection("{\"connectionId\": \"%s\"}".formatted(UUID.randomUUID()));

        assertProblem(result, HttpStatus.NOT_FOUND, "connection.not-found");
    }

    @Test
    void rejectsARequestWithoutAConnection() {
        var result = testConnection("{}");

        assertProblem(result, HttpStatus.BAD_REQUEST, "messaging.invalid-request");
    }

    private UUID aKafkaConnection(String address) {
        return createConnection
                .execute(new CreateConnectionCommand(
                        new ConnectionName("Kafka " + UUID.randomUUID()),
                        BrokerType.KAFKA,
                        new BrokerAddress(address),
                        Credentials.none()))
                .id();
    }

    private MvcTestResult testConnection(String json) {
        return mvc.post()
                .uri("/api/v1/connection-tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static void assertProblem(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"status\": %d, \"code\": \"%s\"}".formatted(status.value(), code));
    }
}
