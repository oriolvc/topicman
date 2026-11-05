package com.oriolvc.topicman.collection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.exception.UnresolvedVariablesException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class TemplateResolverTest {

    private final FixedDynamicValues dynamicValues = new FixedDynamicValues();
    private final TemplateResolver resolver = new TemplateResolver(dynamicValues);

    @Test
    void resolvesEveryTextAMessageIsMadeOf() {
        var headers = new LinkedHashMap<String, String>();
        headers.put("x-{{team}}", "{{env}}");
        var template = MessageTemplate.create(
                TestIds.templateId(),
                new TemplateName("Order created"),
                BrokerType.RABBITMQ,
                TemplateDestination.exchange("{{team}}-events", "orders.{{env}}"),
                null,
                headers,
                new TemplatePayload(PayloadFormat.JSON, "{\"env\": \"{{env}}\"}"));

        var resolved = resolver.resolve(template, anEnvironment(Map.of("team", "billing", "env", "stg")));

        assertThat(resolved.brokerType()).isEqualTo(BrokerType.RABBITMQ);
        assertThat(resolved.destination()).isEqualTo(TemplateDestination.exchange("billing-events", "orders.stg"));
        assertThat(resolved.headers()).containsExactly(Map.entry("x-billing", "stg"));
        assertThat(resolved.payload()).isEqualTo(new TemplatePayload(PayloadFormat.JSON, "{\"env\": \"stg\"}"));
    }

    @Test
    void resolvesTheKey() {
        var resolved = resolver.resolve(aKafkaTemplate("{{orderId}}", "x"), anEnvironment(Map.of("orderId", "42")));

        assertThat(resolved.key()).isEqualTo("42");
    }

    @Test
    void allowsSpacesInsideTheBraces() {
        var resolved = resolver.resolve(aKafkaTemplate(null, "{{ env }}-{{env}}"), anEnvironment(Map.of("env", "stg")));

        assertThat(resolved.payload().content()).isEqualTo("stg-stg");
    }

    @Test
    void insertsValuesAsWrittenWithoutResolvingThemAgain() {
        var resolved = resolver.resolve(
                aKafkaTemplate(null, "{{a}}"), anEnvironment(Map.of("a", "{{b}} costs $5 \\o/", "b", "never")));

        assertThat(resolved.payload().content()).isEqualTo("{{b}} costs $5 \\o/");
    }

    @Test
    void listsEveryMissingVariableAtOnce() {
        var template = MessageTemplate.create(
                TestIds.templateId(),
                new TemplateName("Order created"),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders-{{env}}"),
                "{{orderId}}",
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "{{orderId}} for {{customer}} in {{env}}"));

        assertThatThrownBy(() -> resolver.resolve(template, anEnvironment(Map.of("customer", "acme"))))
                .isInstanceOfSatisfying(UnresolvedVariablesException.class, failure -> {
                    assertThat(failure.missing()).containsExactly("env", "orderId");
                    assertThat(failure.unknownBuiltIns()).isEmpty();
                })
                .hasMessageContaining("env")
                .hasMessageContaining("orderId");
    }

    @Test
    void resolvesWithoutAnEnvironment() {
        var resolved = resolver.resolve(aKafkaTemplate(null, "plain {{$randomInt}}"), null);

        assertThat(resolved.payload().content()).isEqualTo("plain 7");
    }

    @Test
    void failsWithoutAnEnvironmentWhenAVariableIsNeeded() {
        assertThatThrownBy(() -> resolver.resolve(aKafkaTemplate(null, "{{env}}"), null))
                .isInstanceOfSatisfying(
                        UnresolvedVariablesException.class,
                        failure -> assertThat(failure.missing()).containsExactly("env"));
    }

    @Test
    void generatesBuiltInVariables() {
        var resolved = resolver.resolve(
                aKafkaTemplate(null, "{{$uuid}} {{$timestamp}} {{$isoTimestamp}} {{$randomInt}}"), null);

        assertThat(resolved.payload().content())
                .isEqualTo(FixedDynamicValues.UUID_VALUE + " "
                        + FixedDynamicValues.NOW.toEpochMilli() + " "
                        + "2026-11-03T10:15:30.123Z" + " "
                        + FixedDynamicValues.RANDOM_INT);
    }

    @Test
    void givesABuiltInVariableOneValuePerResolution() {
        var resolved = resolver.resolve(
                aKafkaTemplate(
                        "{{$uuid}}", "{\"id\": \"{{$uuid}}\", \"at\": {{$timestamp}}, \"iso\": \"{{$isoTimestamp}}\"}"),
                null);

        assertThat(resolved.key()).isEqualTo(FixedDynamicValues.UUID_VALUE.toString());
        assertThat(dynamicValues.uuids).isEqualTo(1);
        assertThat(dynamicValues.nows).isEqualTo(1);
    }

    @Test
    void generatesFreshValuesOnEveryResolution() {
        resolver.resolve(aKafkaTemplate(null, "{{$uuid}}"), null);
        resolver.resolve(aKafkaTemplate(null, "{{$uuid}}"), null);

        assertThat(dynamicValues.uuids).isEqualTo(2);
    }

    @Test
    void rejectsAnUnknownBuiltInVariable() {
        assertThatThrownBy(() -> resolver.resolve(aKafkaTemplate(null, "{{$guid}} {{env}}"), null))
                .isInstanceOfSatisfying(UnresolvedVariablesException.class, failure -> {
                    assertThat(failure.unknownBuiltIns()).containsExactly("$guid");
                    assertThat(failure.missing()).containsExactly("env");
                });
    }

    @Test
    void leavesTextThatIsNotAPlaceholderAsWritten() {
        var payload = "{{ not a name }} {{ {{1x}} {{}} }} {single}";

        var resolved = resolver.resolve(aKafkaTemplate(null, payload), null);

        assertThat(resolved.payload().content()).isEqualTo(payload);
    }

    @Test
    void dropsAKeyThatResolvesToNothing() {
        var resolved = resolver.resolve(aKafkaTemplate("{{orderId}}", "x"), anEnvironment(Map.of("orderId", "")));

        assertThat(resolved.key()).isNull();
    }

    @Test
    void rejectsADestinationThatResolvesToNothing() {
        var template = MessageTemplate.create(
                TestIds.templateId(),
                new TemplateName("Blank topic"),
                BrokerType.KAFKA,
                TemplateDestination.topic("{{topic}}"),
                null,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "x"));

        assertThatThrownBy(() -> resolver.resolve(template, anEnvironment(Map.of("topic", " "))))
                .isInstanceOf(InvalidTemplateException.class);
    }

    private static MessageTemplate aKafkaTemplate(@Nullable String key, String payload) {
        return MessageTemplate.create(
                TestIds.templateId(),
                new TemplateName("Template"),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders"),
                key,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, payload));
    }

    private static Environment anEnvironment(Map<String, String> values) {
        var variables = new LinkedHashMap<VariableName, String>();
        values.forEach((name, value) -> variables.put(new VariableName(name), value));
        return Environment.create(
                TestIds.environmentId(), new EnvironmentName("test"), variables, Instant.parse("2026-11-03T10:00:00Z"));
    }
}
