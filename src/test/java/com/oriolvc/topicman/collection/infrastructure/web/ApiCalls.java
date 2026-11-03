package com.oriolvc.topicman.collection.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Request and assertion helpers shared by the collection context's API tests. */
final class ApiCalls {

    static final String COLLECTIONS = "/api/v1/collections";
    static final String ENVIRONMENTS = "/api/v1/environments";

    private final MockMvcTester mvc;

    ApiCalls(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    MvcTestResult post(String uri, String json) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    MvcTestResult put(String uri, String json) {
        return mvc.put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    MvcTestResult patch(String uri, String json) {
        return mvc.patch()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    MvcTestResult get(String uri) {
        return mvc.get().uri(uri).exchange();
    }

    MvcTestResult delete(String uri) {
        return mvc.delete().uri(uri).exchange();
    }

    /** Creates a collection and returns its id. */
    String aCollection(String name) {
        return idOf(post(COLLECTIONS, "{\"name\": \"%s\"}".formatted(name)));
    }

    /** Adds a Kafka template using {{env}}, {{orderId}} and {{$uuid}} and returns its id. */
    String aKafkaTemplate(String collectionId, String name) {
        return idOf(post(COLLECTIONS + "/" + collectionId + "/templates", kafkaTemplate(name)));
    }

    static String kafkaTemplate(String name) {
        return """
                {"name": "%s", "brokerType": "KAFKA",
                 "destination": {"type": "TOPIC", "name": "orders-{{env}}"},
                 "key": "{{orderId}}",
                 "headers": {"trace-id": "{{$uuid}}", "source": "topicman"},
                 "payload": {"format": "JSON", "content": "{\\"id\\": \\"{{orderId}}\\"}"}}
                """.formatted(name);
    }

    /** Creates an environment and returns its id. */
    String anEnvironment(String name, String variablesJson) {
        return idOf(post(ENVIRONMENTS, "{\"name\": \"%s\", \"variables\": %s}".formatted(name, variablesJson)));
    }

    static String idOf(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        try {
            // Ids are JSON integers (ADR 0007); tests use them in URLs and bodies as text.
            return String.valueOf(JsonPath.<Object>read(result.getResponse().getContentAsString(), "$.id"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void assertProblem(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"status\": %d, \"code\": \"%s\"}".formatted(status.value(), code));
    }
}
