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
class CollectionsApiIT {

    private final ApiCalls api;
    private final JdbcTemplate jdbc;

    CollectionsApiIT(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.api = new ApiCalls(mvc);
        this.jdbc = jdbc;
    }

    @BeforeEach
    void deleteAllCollectionsAndEnvironments() {
        jdbc.update("DELETE FROM collection");
        jdbc.update("DELETE FROM environment");
    }

    @Test
    void createsAnEmptyCollection() {
        var result = api.post(COLLECTIONS, "{\"name\": \"  Orders  \"}");

        var id = idOf(result);
        assertThat(result).headers().hasValue("Location", COLLECTIONS + "/" + id);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"id\": \"%s\", \"name\": \"Orders\", \"templates\": []}".formatted(id));
    }

    @Test
    void rejectsATakenName() {
        api.aCollection("Orders");

        assertProblem(api.post(COLLECTIONS, "{\"name\": \"Orders\"}"), HttpStatus.CONFLICT, "collection.name-taken");
    }

    @Test
    void rejectsABlankName() {
        assertProblem(
                api.post(COLLECTIONS, "{\"name\": \"   \"}"), HttpStatus.BAD_REQUEST, "collection.invalid-collection");
        assertProblem(api.post(COLLECTIONS, "{}"), HttpStatus.BAD_REQUEST, "collection.invalid-request");
    }

    @Test
    void listsSummariesOrderedByName() {
        var shipping = api.aCollection("Shipping");
        api.aCollection("Billing");
        api.aKafkaTemplate(shipping, "Shipped");

        var result = api.get(COLLECTIONS);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Billing", "Shipping");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[*].templateCount")
                .asArray()
                .containsExactly(0, 1);
        assertThat(result).bodyText().doesNotContain("templates\"");
    }

    @Test
    void getsACollectionWithItsTemplates() {
        var id = api.aCollection("Orders");
        api.aKafkaTemplate(id, "Order created");
        api.aKafkaTemplate(id, "Order shipped");

        var result = api.get(COLLECTIONS + "/" + id);

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.templates[*].name")
                .asArray()
                .containsExactly("Order created", "Order shipped");
    }

    @Test
    void renamesACollection() {
        var id = api.aCollection("Orders");
        api.aCollection("Shipping");

        assertThat(api.patch(COLLECTIONS + "/" + id, "{\"name\": \"Billing\"}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Billing");
        assertProblem(
                api.patch(COLLECTIONS + "/" + id, "{\"name\": \"Shipping\"}"),
                HttpStatus.CONFLICT,
                "collection.name-taken");
    }

    @Test
    void deletesACollection() {
        var id = api.aCollection("Orders");
        api.aKafkaTemplate(id, "Order created");

        assertThat(api.delete(COLLECTIONS + "/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertProblem(api.get(COLLECTIONS + "/" + id), HttpStatus.NOT_FOUND, "collection.not-found");
        assertProblem(api.delete(COLLECTIONS + "/" + id), HttpStatus.NOT_FOUND, "collection.not-found");
    }

    @Test
    void answersNotFoundForAnUnknownCollection() {
        var unknown = COLLECTIONS + "/" + UUID.randomUUID();

        assertProblem(api.get(unknown), HttpStatus.NOT_FOUND, "collection.not-found");
        assertProblem(api.patch(unknown, "{\"name\": \"x\"}"), HttpStatus.NOT_FOUND, "collection.not-found");
    }
}
