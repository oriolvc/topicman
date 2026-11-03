package com.oriolvc.topicman.collection.infrastructure.web;

import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.COLLECTIONS;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.ENVIRONMENTS;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.assertProblem;
import static com.oriolvc.topicman.collection.infrastructure.web.ApiCalls.idOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class FilesApiIT {

    private static final String YAML = "application/yaml";

    private final MockMvcTester mvc;
    private final ApiCalls api;
    private final JdbcTemplate jdbc;

    FilesApiIT(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.api = new ApiCalls(mvc);
        this.jdbc = jdbc;
    }

    @BeforeEach
    void deleteAllCollectionsAndEnvironments() {
        jdbc.update("DELETE FROM collection");
        jdbc.update("DELETE FROM environment");
    }

    @Test
    void exportsACollectionAsAPortableFile() throws Exception {
        var collection = api.aCollection("Orders and Shipping");
        api.aKafkaTemplate(collection, "Order created");

        var result = mvc.get().uri(COLLECTIONS + "/" + collection + "/export").exchange();

        assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(YAML);
        assertThat(result)
                .headers()
                .hasValue("Content-Disposition", "attachment; filename=\"orders-and-shipping.topicman.yaml\"");
        var file = result.getResponse().getContentAsString();
        assertThat(file)
                .startsWith("topicman: 1\nkind: collection\nname: Orders and Shipping\ntemplates:\n")
                .contains(
                        "    destination:\n      type: TOPIC\n      name: \"orders-{{env}}\"\n    key: \"{{orderId}}\"\n")
                .doesNotContainPattern("(?m)^\\s*(- )?id:");
    }

    @Test
    void importsWhatItExports() throws Exception {
        var collection = api.aCollection("Orders");
        api.aKafkaTemplate(collection, "Order created");
        api.post(COLLECTIONS + "/" + collection + "/templates", """
                {"name": "Broadcast", "brokerType": "RABBITMQ",
                 "destination": {"type": "EXCHANGE", "name": "events", "routingKey": ""},
                 "payload": {"format": "TEXT", "content": "line one\\nline two\\n"}}
                """);
        var file = mvc.get()
                .uri(COLLECTIONS + "/" + collection + "/export")
                .exchange()
                .getResponse()
                .getContentAsString();

        var imported = importCollection(file, "?name=Orders copy");

        var copy = idOf(imported);
        assertThat(imported).headers().hasValue("Location", COLLECTIONS + "/" + copy);
        assertThat(templatesOf(copy)).isEqualTo(templatesOf(collection));
        assertThat(api.get(COLLECTIONS + "/" + copy))
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Orders copy");
    }

    @Test
    void importsAHandWrittenFile() {
        var result = importCollection("""
                topicman: 1
                kind: collection
                name: Orders
                templates:
                  - name: Order created
                    brokerType: KAFKA
                    destination: { type: TOPIC, name: "orders-{{env}}" }
                    key: "{{orderId}}"
                    headers:
                      trace-id: "{{$uuid}}"
                      source: topicman
                    payload:
                      format: JSON
                      content: |
                        {"id": "{{orderId}}"}
                """, "");

        var id = idOf(result);
        assertThat(api.get(COLLECTIONS + "/" + id)).bodyJson().isLenientlyEqualTo("""
                {"name": "Orders", "templates": [{
                  "name": "Order created", "brokerType": "KAFKA",
                  "destination": {"type": "TOPIC", "name": "orders-{{env}}"},
                  "key": "{{orderId}}",
                  "headers": {"trace-id": "{{$uuid}}", "source": "topicman"},
                  "payload": {"format": "JSON", "content": "{\\"id\\": \\"{{orderId}}\\"}\\n"}}]}
                """);
        assertThat(api.get(COLLECTIONS + "/" + id)).bodyText().contains("\"trace-id\":\"{{$uuid}}\",\"source\"");
    }

    @Test
    void rejectsATakenName() {
        api.aCollection("Orders");

        assertProblem(
                importCollection(emptyCollectionFile("Orders"), ""), HttpStatus.CONFLICT, "collection.name-taken");
    }

    @Test
    void rejectsAFileThatIsNotACollectionFileOfThisVersion() {
        assertProblem(
                importCollection("templates: [unclosed", ""), HttpStatus.BAD_REQUEST, "collection.invalid-request");
        assertProblem(
                importCollection("topicman: 1\nkind: environment\nname: x\ntemplates: []\n", ""),
                HttpStatus.BAD_REQUEST,
                "collection.invalid-request");
        assertProblem(
                importCollection("topicman: 2\nkind: collection\nname: x\ntemplates: []\n", ""),
                HttpStatus.BAD_REQUEST,
                "collection.invalid-request");
    }

    @Test
    void rejectsAFileWithTwoTemplatesOfTheSameName() {
        var template = """
                  - name: Order created
                    brokerType: KAFKA
                    destination: { type: TOPIC, name: orders }
                    payload: { format: TEXT, content: x }
                """;

        assertProblem(
                importCollection(
                        emptyCollectionFile("Orders").replace("templates: []\n", "templates:\n" + template + template),
                        ""),
                HttpStatus.BAD_REQUEST,
                "collection.invalid-template");
        assertThat(api.get(COLLECTIONS)).bodyJson().extractingPath("$.length()").isEqualTo(0);
    }

    @Test
    void answersNotFoundForAnUnknownCollection() {
        assertProblem(
                mvc.get().uri(COLLECTIONS + "/999999999/export").exchange(),
                HttpStatus.NOT_FOUND,
                "collection.not-found");
    }

    @Test
    void exportsAndImportsAnEnvironment() throws Exception {
        var staging = api.anEnvironment(
                "staging", "[{\"name\": \"env\", \"value\": \"stg\"}, {\"name\": \"suffix\", \"value\": \"\"}]");

        var exported = mvc.get().uri(ENVIRONMENTS + "/" + staging + "/export").exchange();

        assertThat(exported).hasStatusOk().hasContentTypeCompatibleWith(YAML);
        assertThat(exported)
                .headers()
                .hasValue("Content-Disposition", "attachment; filename=\"staging.topicman.yaml\"");
        var file = exported.getResponse().getContentAsString();
        assertThat(file).startsWith("topicman: 1\nkind: environment\nname: staging\nvariables:\n");
        var imported = post(ENVIRONMENTS + "/import?name=staging copy", file);
        assertThat(api.get(ENVIRONMENTS + "/" + idOf(imported))).bodyJson().isLenientlyEqualTo("""
                        {"name": "staging copy",
                         "variables": [{"name": "env", "value": "stg"}, {"name": "suffix", "value": ""}]}
                        """);
    }

    @Test
    void rejectsAnEnvironmentFileWithInvalidVariables() {
        var file = """
                topicman: 1
                kind: environment
                name: staging
                variables:
                  - { name: env, value: stg }
                  - { name: env, value: prd }
                """;

        assertProblem(post(ENVIRONMENTS + "/import", file), HttpStatus.BAD_REQUEST, "collection.invalid-environment");
    }

    private MvcTestResult importCollection(String file, String query) {
        return post(COLLECTIONS + "/import" + query, file);
    }

    private MvcTestResult post(String uri, String file) {
        return mvc.post().uri(uri).contentType(YAML).content(file).exchange();
    }

    private static String emptyCollectionFile(String name) {
        return "topicman: 1\nkind: collection\nname: " + name + "\ntemplates: []\n";
    }

    /** The collection's templates as JSON, without their ids. */
    private Object templatesOf(String collectionId) throws Exception {
        var json = api.get(COLLECTIONS + "/" + collectionId).getResponse().getContentAsString();
        return JsonPath.parse(json).delete("$.templates[*].id").read("$.templates");
    }
}
