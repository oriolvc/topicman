package com.oriolvc.topicman.messaging.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.connection.application.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.messaging.application.ConsumeSessionRegistry;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(
        properties = {
            "topicman.consume-sessions.max-open=2",
            "topicman.consume-sessions.idle-timeout=1s",
            "topicman.consume-sessions.expiry-check-interval=200ms"
        })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
@TestConstructor(autowireMode = AutowireMode.ALL)
class ConsumeSessionLimitsIT {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final CreateConnectionUseCase createConnection;
    private final ConsumeSessionRegistry registry;

    ConsumeSessionLimitsIT(
            MockMvcTester mvc,
            JdbcTemplate jdbc,
            CreateConnectionUseCase createConnection,
            ConsumeSessionRegistry registry) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.createConnection = createConnection;
        this.registry = registry;
    }

    @BeforeEach
    void deleteAllConnections() {
        jdbc.update("DELETE FROM broker_connection");
    }

    @AfterEach
    void closeAllSessions() {
        registry.close();
    }

    @Test
    void rejectsOpeningMoreSessionsThanTheLimit() {
        var connection = aKafkaConnectionWithTopic("orders");
        assertThat(open(connection, "orders")).hasStatus(HttpStatus.CREATED);
        assertThat(open(connection, "orders")).hasStatus(HttpStatus.CREATED);

        var third = open(connection, "orders");

        assertThat(third).hasStatus(HttpStatus.TOO_MANY_REQUESTS).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(third).bodyJson().extractingPath("$.code").isEqualTo("messaging.session-limit-reached");
    }

    @Test
    void closesSessionsNoClientStreamsAfterTheIdleTimeout() {
        var connection = aKafkaConnectionWithTopic("orders");
        assertThat(open(connection, "orders")).hasStatus(HttpStatus.CREATED);
        assertThat(open(connection, "orders")).hasStatus(HttpStatus.CREATED);
        assertThat(open(connection, "orders")).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(open(connection, "orders")).hasStatus(HttpStatus.CREATED));
    }

    private UUID aKafkaConnectionWithTopic(String topic) {
        var connection = createConnection
                .execute(new CreateConnectionCommand(
                        new ConnectionName("Kafka " + UUID.randomUUID()),
                        BrokerType.KAFKA,
                        new BrokerAddress(KAFKA.getBootstrapServers()),
                        Credentials.none()))
                .id();
        assertThat(mvc.post()
                        .uri("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "%s"},
                                 "payload": {"format": "TEXT", "content": "creates the topic"}}
                                """.formatted(connection, topic)))
                .hasStatusOk();
        return connection;
    }

    private MvcTestResult open(UUID connection, String topic) {
        return mvc.post()
                .uri("/api/v1/consume-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"connectionId": "%s", "destination": {"type": "TOPIC", "name": "%s"}}
                        """.formatted(connection, topic))
                .exchange();
    }
}
