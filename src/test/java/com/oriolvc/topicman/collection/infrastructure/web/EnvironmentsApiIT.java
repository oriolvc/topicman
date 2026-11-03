package com.oriolvc.topicman.collection.infrastructure.web;

import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.ENVIRONMENTS;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.assertProblem;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.idOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
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
class EnvironmentsApiIT {

    private final ApiCalls api;
    private final JdbcTemplate jdbc;

    EnvironmentsApiIT(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.api = new ApiCalls(mvc);
        this.jdbc = jdbc;
    }

    @BeforeEach
    void deleteAllCollectionsAndEnvironments() {
        jdbc.update("DELETE FROM collection");
        jdbc.update("DELETE FROM environment");
    }

    @Test
    void createsAnEnvironmentWithItsVariablesInOrder() {
        var result = api.post(ENVIRONMENTS, """
                {"name": "staging", "variables": [{"name": "zone", "value": "eu"}, {"name": "app.suffix", "value": ""}]}
                """);

        var id = idOf(result);
        assertThat(result).headers().hasValue("Location", ENVIRONMENTS + "/" + id);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"id": "%s", "name": "staging",
                 "variables": [{"name": "zone", "value": "eu"}, {"name": "app.suffix", "value": ""}]}
                """.formatted(id));
        assertThat(result)
                .bodyJson()
                .extractingPath("$.variables[*].name")
                .asArray()
                .containsExactly("zone", "app.suffix");
    }

    @Test
    void rejectsInvalidVariables() {
        assertProblem(
                api.post(
                        ENVIRONMENTS,
                        "{\"name\": \"x\", \"variables\": [{\"name\": \"a\", \"value\": \"1\"}, {\"name\": \"a\", \"value\": \"2\"}]}"),
                HttpStatus.BAD_REQUEST,
                "collection.invalid-environment");
        assertProblem(
                api.post(ENVIRONMENTS, "{\"name\": \"x\", \"variables\": [{\"name\": \"$uuid\", \"value\": \"1\"}]}"),
                HttpStatus.BAD_REQUEST,
                "collection.invalid-request");
    }

    @Test
    void rejectsATakenName() {
        api.anEnvironment("staging", "[]");

        assertProblem(
                api.post(ENVIRONMENTS, "{\"name\": \"staging\", \"variables\": []}"),
                HttpStatus.CONFLICT,
                "collection.environment-name-taken");
    }

    @Test
    void listsGetsReplacesAndDeletesEnvironments() {
        var staging = api.anEnvironment("staging", "[{\"name\": \"env\", \"value\": \"stg\"}]");
        api.anEnvironment("dev", "[]");

        assertThat(api.get(ENVIRONMENTS))
                .bodyJson()
                .extractingPath("$[*].name")
                .asArray()
                .containsExactly("dev", "staging");
        assertThat(api.get(ENVIRONMENTS + "/" + staging))
                .bodyJson()
                .extractingPath("$.variables[0].value")
                .isEqualTo("stg");
        assertThat(api.put(ENVIRONMENTS + "/" + staging, """
                        {"name": "stage", "variables": [{"name": "region", "value": "eu"}]}
                        """))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo(
                        "{\"name\": \"stage\", \"variables\": [{\"name\": \"region\", \"value\": \"eu\"}]}");
        assertProblem(
                api.put(ENVIRONMENTS + "/" + staging, "{\"name\": \"dev\", \"variables\": []}"),
                HttpStatus.CONFLICT,
                "collection.environment-name-taken");
        assertThat(api.delete(ENVIRONMENTS + "/" + staging)).hasStatus(HttpStatus.NO_CONTENT);
        assertProblem(api.get(ENVIRONMENTS + "/" + staging), HttpStatus.NOT_FOUND, "collection.environment-not-found");
    }
}
