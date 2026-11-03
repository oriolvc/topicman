package com.oriolvc.topicman.collection.infrastructure.web;

import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.COLLECTIONS;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.assertProblem;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.idOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class TemplatesApiIT {

    private final ApiCalls api;
    private final JdbcTemplate jdbc;

    TemplatesApiIT(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.api = new ApiCalls(mvc);
        this.jdbc = jdbc;
    }

    @BeforeEach
    void deleteAllCollectionsAndEnvironments() {
        jdbc.update("DELETE FROM collection");
        jdbc.update("DELETE FROM environment");
    }

    @Test
    void addsATemplate() {
        var collection = api.aCollection("Orders");

        var result = api.post(templates(collection), ApiCalls.kafkaTemplate("Order created"));

        var id = idOf(result);
        assertThat(result).headers().hasValue("Location", templates(collection) + "/" + id);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"id": "%s", "name": "Order created", "brokerType": "KAFKA",
                 "destination": {"type": "TOPIC", "name": "orders-{{env}}"},
                 "key": "{{orderId}}",
                 "headers": {"trace-id": "{{$uuid}}", "source": "topicman"},
                 "payload": {"format": "JSON", "content": "{\\"id\\": \\"{{orderId}}\\"}"}}
                """.formatted(id));
        assertThat(result).bodyText().contains("\"trace-id\":\"{{$uuid}}\",\"source\":\"topicman\"");
    }

    @Test
    void omitsTheKeyOfATemplateWithout() {
        var collection = api.aCollection("Orders");

        var result = api.post(templates(collection), """
                {"name": "Broadcast", "brokerType": "RABBITMQ",
                 "destination": {"type": "EXCHANGE", "name": "events", "routingKey": ""},
                 "payload": {"format": "TEXT", "content": "hello"}}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyText().doesNotContain("\"key\"");
        assertThat(result).bodyJson().extractingPath("$.headers").isEqualTo(java.util.Map.of());
    }

    @Test
    void rejectsATemplateThatDoesNotFitItsBroker() {
        var collection = api.aCollection("Orders");

        assertProblem(api.post(templates(collection), """
                        {"name": "Wrong", "brokerType": "RABBITMQ", "destination": {"type": "TOPIC", "name": "orders"},
                         "payload": {"format": "TEXT", "content": "x"}}
                        """), HttpStatus.BAD_REQUEST, "collection.invalid-template");
        assertProblem(api.post(templates(collection), """
                        {"name": "Keyed", "brokerType": "RABBITMQ",
                         "destination": {"type": "EXCHANGE", "name": "events", "routingKey": ""},
                         "key": "k", "payload": {"format": "TEXT", "content": "x"}}
                        """), HttpStatus.BAD_REQUEST, "collection.invalid-template");
    }

    @Test
    void rejectsATakenTemplateName() {
        var collection = api.aCollection("Orders");
        api.aKafkaTemplate(collection, "Order created");

        assertProblem(
                api.post(templates(collection), ApiCalls.kafkaTemplate("Order created")),
                HttpStatus.CONFLICT,
                "collection.template-name-taken");
    }

    @Test
    void replacesATemplate() {
        var collection = api.aCollection("Orders");
        var id = api.aKafkaTemplate(collection, "Order created");

        var result = api.put(templates(collection) + "/" + id, """
                {"name": "Order placed", "brokerType": "PULSAR", "destination": {"type": "TOPIC", "name": "placed"},
                 "payload": {"format": "TEXT", "content": "placed"}}
                """);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"id": "%s", "name": "Order placed", "brokerType": "PULSAR"}
                """.formatted(id));
        assertProblem(
                api.put(templates(collection) + "/" + UUID.randomUUID(), ApiCalls.kafkaTemplate("x")),
                HttpStatus.NOT_FOUND,
                "collection.template-not-found");
    }

    @Test
    void deletesATemplate() {
        var collection = api.aCollection("Orders");
        var id = api.aKafkaTemplate(collection, "Order created");

        assertThat(api.delete(templates(collection) + "/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertProblem(
                api.delete(templates(collection) + "/" + id), HttpStatus.NOT_FOUND, "collection.template-not-found");
    }

    @Test
    void resolvesATemplateAgainstAnEnvironment() {
        var collection = api.aCollection("Orders");
        var template = api.aKafkaTemplate(collection, "Order created");
        var staging = api.anEnvironment(
                "staging", "[{\"name\": \"env\", \"value\": \"stg\"}, {\"name\": \"orderId\", \"value\": \"42\"}]");

        var result = api.post(resolution(collection, template), "{\"environmentId\": \"%s\"}".formatted(staging));

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"brokerType": "KAFKA", "destination": {"type": "TOPIC", "name": "orders-stg"}, "key": "42",
                 "headers": {"source": "topicman"},
                 "payload": {"format": "JSON", "content": "{\\"id\\": \\"42\\"}"}}
                """);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.headers.trace-id")
                .asString()
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void listsEveryMissingVariable() {
        var collection = api.aCollection("Orders");
        var template = api.aKafkaTemplate(collection, "Order created");

        var result = api.post(resolution(collection, template), "{}");

        assertProblem(result, HttpStatus.UNPROCESSABLE_CONTENT, "collection.unresolved-variables");
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"missing\": [\"env\", \"orderId\"], \"unknownBuiltIns\": []}");
    }

    @Test
    void answersNotFoundForAnUnknownEnvironment() {
        var collection = api.aCollection("Orders");
        var template = api.aKafkaTemplate(collection, "Order created");

        assertProblem(
                api.post(resolution(collection, template), "{\"environmentId\": \"%s\"}".formatted(UUID.randomUUID())),
                HttpStatus.NOT_FOUND,
                "collection.environment-not-found");
    }

    private static String templates(String collectionId) {
        return COLLECTIONS + "/" + collectionId + "/templates";
    }

    private static String resolution(String collectionId, String templateId) {
        return templates(collectionId) + "/" + templateId + "/resolution";
    }
}
