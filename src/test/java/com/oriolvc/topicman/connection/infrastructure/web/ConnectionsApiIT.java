package com.oriolvc.topicman.connection.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.TestcontainersConfiguration;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class ConnectionsApiIT {

    private static final String CONNECTIONS = "/api/v1/connections";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;

    ConnectionsApiIT(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void deleteAllConnections() {
        jdbc.update("DELETE FROM broker_connection");
    }

    @Test
    void createsAConnection() {
        var result = create("""
                {"name": "Local Kafka", "brokerType": "KAFKA", "address": "localhost:9092",
                 "credentials": {"type": "USERNAME_PASSWORD", "username": "app", "password": "s3cret-pw"}}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        var id = idOf(result);
        assertThat(result).headers().hasValue("Location", CONNECTIONS + "/" + id);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                        {"id": %s, "name": "Local Kafka", "brokerType": "KAFKA", "address": "localhost:9092",
                         "credentialsType": "USERNAME_PASSWORD", "saslMechanism": "PLAIN", "tls": {"enabled": false}}
                        """.formatted(id));
        assertThat(result).bodyJson().doesNotHavePath("$.tls.trustedCertificates");
        assertThat(result)
                .bodyJson()
                .hasPathSatisfying("$.createdAt", path -> path.assertThat().isNotNull());
    }

    @Test
    void neverReturnsCredentials() throws Exception {
        var id = idOf(create(aPulsarConnectionWithToken("Local Pulsar", "jwt-s3cret")));

        assertThat(create(aPulsarConnectionWithToken("Other Pulsar", "jwt-s3cret"))
                        .getResponse()
                        .getContentAsString())
                .doesNotContain("jwt-s3cret");
        assertThat(mvc.get().uri(CONNECTIONS + "/" + id)).bodyText().doesNotContain("jwt-s3cret");
        assertThat(mvc.get().uri(CONNECTIONS)).bodyText().doesNotContain("jwt-s3cret");
    }

    @Test
    void createsAKafkaConnectionWithTlsAndScram() {
        var result = create("""
                {"name": "Secure Kafka", "brokerType": "KAFKA", "address": "kafka.example.com:9093",
                 "credentials": {"type": "USERNAME_PASSWORD", "username": "app", "password": "pw",
                                 "saslMechanism": "SCRAM_SHA_512"},
                 "tls": {"enabled": true, "trustedCertificates": %s}}
                """.formatted(JSON.writeValueAsString(TestCertificates.CA)));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"saslMechanism": "SCRAM_SHA_512", "tls": {"enabled": true, "trustedCertificates": %s}}
                """.formatted(
                        JSON.writeValueAsString(TestCertificates.CA.strip())));
    }

    @Test
    void returnsNoSaslMechanismForBrokersWithoutOne() {
        var result = create("""
                {"name": "Local RabbitMQ", "brokerType": "RABBITMQ", "address": "amqps://localhost:5671",
                 "credentials": {"type": "USERNAME_PASSWORD", "username": "guest", "password": "guest"}}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().doesNotHavePath("$.saslMechanism");
    }

    @Test
    void rejectsASaslMechanismForRabbitMq() {
        var result = create("""
                {"name": "Local RabbitMQ", "brokerType": "RABBITMQ", "address": "amqp://localhost:5672",
                 "credentials": {"type": "USERNAME_PASSWORD", "username": "guest", "password": "guest",
                                 "saslMechanism": "PLAIN"}}
                """);

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-connection");
    }

    @Test
    void rejectsTheTlsFlagForPulsar() {
        var result = create("""
                {"name": "Local Pulsar", "brokerType": "PULSAR", "address": "pulsar://localhost:6650",
                 "credentials": {"type": "NONE"}, "tls": {"enabled": true}}
                """);

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-connection");
    }

    @Test
    void rejectsTrustedCertificatesThatAreNotPem() {
        var result = create("""
                {"name": "Secure Kafka", "brokerType": "KAFKA", "address": "kafka:9093",
                 "credentials": {"type": "NONE"}, "tls": {"enabled": true, "trustedCertificates": "not a certificate"}}
                """);

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-connection");
    }

    @Test
    void changesOnlyTheTlsSettings() {
        var id = idOf(create(aKafkaConnection("Local Kafka")));

        var result = patch(id, """
                {"tls": {"enabled": true}}
                """);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"name": "Local Kafka", "credentialsType": "NONE", "tls": {"enabled": true}}
                """);
    }

    @Test
    void listsConnectionsOrderedByName() {
        create(aKafkaConnection("Staging"));
        create(aKafkaConnection("Local"));

        assertThat(mvc.get().uri(CONNECTIONS))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].name")
                .asArray()
                .containsExactly("Local", "Staging");
    }

    @Test
    void getsAConnection() {
        var id = idOf(create(aKafkaConnection("Local")));

        assertThat(mvc.get().uri(CONNECTIONS + "/" + id))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Local");
    }

    @Test
    void answersNotFoundWithAProblem() {
        var result = mvc.get().uri(CONNECTIONS + "/999999999").exchange();

        assertProblem(result, HttpStatus.NOT_FOUND, "connection.not-found");
    }

    @Test
    void rejectsADuplicateName() {
        create(aKafkaConnection("Local"));

        assertProblem(create(aKafkaConnection("Local")), HttpStatus.CONFLICT, "connection.name-taken");
    }

    @Test
    void rejectsCredentialsTheBrokerDoesNotSupport() {
        var result = create("""
                {"name": "Kafka", "brokerType": "KAFKA", "address": "localhost:9092",
                 "credentials": {"type": "TOKEN", "token": "jwt"}}
                """);

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-connection");
    }

    @Test
    void rejectsABlankName() {
        assertProblem(create(aKafkaConnection("   ")), HttpStatus.BAD_REQUEST, "connection.invalid-connection");
    }

    @Test
    void rejectsARequestThatBreaksTheSchemaWithoutEchoingIt() {
        var result = create("""
                {"brokerType": "KAFKA", "address": "localhost:9092",
                 "credentials": {"type": "USERNAME_PASSWORD", "username": "", "password": "s3cret-pw"}}
                """);

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-request");
        assertThat(result).bodyText().doesNotContain("s3cret-pw");
    }

    @Test
    void rejectsMalformedJsonWithoutEchoingIt() {
        var result = create("{\"name\": \"Kafka\", \"password\": \"s3cret-pw\"");

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-request");
        assertThat(result).bodyText().doesNotContain("s3cret-pw");
    }

    @Test
    void rejectsAnUnknownCredentialsType() {
        var result = create("""
                {"name": "Kafka", "brokerType": "KAFKA", "address": "localhost:9092",
                 "credentials": {"type": "CERTIFICATE"}}
                """);

        assertProblem(result, HttpStatus.BAD_REQUEST, "connection.invalid-request");
    }

    @Test
    void updatesOnlyTheGivenProperties() {
        var id = idOf(create(aPulsarConnectionWithToken("Local Pulsar", "jwt")));

        var result = patch(id, "{\"name\": \"Staging Pulsar\"}");

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                        {"name": "Staging Pulsar", "address": "pulsar://localhost:6650", "credentialsType": "TOKEN"}
                        """);
    }

    @Test
    void rejectsAnEmptyUpdate() {
        var id = idOf(create(aKafkaConnection("Local")));

        assertProblem(patch(id, "{}"), HttpStatus.BAD_REQUEST, "connection.invalid-request");
    }

    @Test
    void rejectsRenamingToANameAnotherConnectionHas() {
        create(aKafkaConnection("Staging"));
        var id = idOf(create(aKafkaConnection("Local")));

        assertProblem(patch(id, "{\"name\": \"Staging\"}"), HttpStatus.CONFLICT, "connection.name-taken");
    }

    @Test
    void deletesAConnection() {
        var id = idOf(create(aKafkaConnection("Local")));

        assertThat(mvc.delete().uri(CONNECTIONS + "/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(CONNECTIONS + "/" + id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsAnIdThatIsNotAPositiveNumber() {
        assertProblem(
                mvc.get()
                        .uri(CONNECTIONS + "/0f8fad5b-d9cb-469f-a165-70867728950e")
                        .exchange(),
                HttpStatus.BAD_REQUEST,
                "connection.invalid-request");
        assertProblem(
                mvc.get().uri(CONNECTIONS + "/0").exchange(), HttpStatus.BAD_REQUEST, "connection.invalid-request");
    }

    @Test
    void answersNotFoundWhenDeletingAnUnknownConnection() {
        var result = mvc.delete().uri(CONNECTIONS + "/999999999").exchange();

        assertProblem(result, HttpStatus.NOT_FOUND, "connection.not-found");
    }

    private MvcTestResult create(String json) {
        return mvc.post()
                .uri(CONNECTIONS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult patch(String id, String json) {
        return mvc.patch()
                .uri(CONNECTIONS + "/" + id)
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

    private static String idOf(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        try {
            // Ids are JSON integers (ADR 0007); tests use them in URLs and bodies as text.
            return String.valueOf(JsonPath.<Object>read(result.getResponse().getContentAsString(), "$.id"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String aKafkaConnection(String name) {
        return """
                {"name": "%s", "brokerType": "KAFKA", "address": "localhost:9092", "credentials": {"type": "NONE"}}
                """.formatted(name);
    }

    private static String aPulsarConnectionWithToken(String name, String token) {
        return """
                {"name": "%s", "brokerType": "PULSAR", "address": "pulsar://localhost:6650",
                 "credentials": {"type": "TOKEN", "token": "%s"}}
                """.formatted(name, token);
    }
}
