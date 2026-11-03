package com.oriolvc.topicman.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageTemplateTest {

    @Test
    void createsATemplateWithTheIdItIsGiven() {
        var template = MessageTemplate.create(
                new TemplateId(42),
                new TemplateName("Order created"),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders"),
                "{{orderId}}",
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "hi"));

        assertThat(template.id()).isEqualTo(new TemplateId(42));
        assertThat(template.name()).isEqualTo(new TemplateName("Order created"));
        assertThat(template.brokerType()).isEqualTo(BrokerType.KAFKA);
        assertThat(template.key()).isEqualTo("{{orderId}}");
    }

    @Test
    void keepsHeadersInTheirOrderAndUnmodifiable() {
        var headers = new LinkedHashMap<String, String>();
        headers.put("trace-id", "{{$uuid}}");
        headers.put("source", "topicman");

        var template = MessageTemplate.create(
                TestIds.templateId(),
                new TemplateName("Order created"),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders"),
                null,
                headers,
                new TemplatePayload(PayloadFormat.TEXT, "hi"));
        headers.put("later", "change");

        assertThat(template.headers())
                .containsExactly(Map.entry("trace-id", "{{$uuid}}"), Map.entry("source", "topicman"));
        assertThatThrownBy(() -> template.headers().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsAnEmptyKey() {
        assertThatThrownBy(() -> MessageTemplate.create(
                        TestIds.templateId(),
                        new TemplateName("No key"),
                        BrokerType.KAFKA,
                        TemplateDestination.topic("orders"),
                        "",
                        Map.of(),
                        new TemplatePayload(PayloadFormat.TEXT, "hi")))
                .isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    void rejectsABlankHeaderName() {
        assertThatThrownBy(() -> MessageTemplate.create(
                        TestIds.templateId(),
                        new TemplateName("Blank header"),
                        BrokerType.KAFKA,
                        TemplateDestination.topic("orders"),
                        null,
                        Map.of(" ", "value"),
                        new TemplatePayload(PayloadFormat.TEXT, "hi")))
                .isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    void renamesAndChangesContentKeepingItsId() {
        var template = aTemplate("Order created");

        var changed = template.withContent(
                new TemplateName("Order shipped"),
                BrokerType.PULSAR,
                TemplateDestination.topic("shipments"),
                null,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "shipped"));

        assertThat(changed.id()).isEqualTo(template.id());
        assertThat(changed.name()).isEqualTo(new TemplateName("Order shipped"));
        assertThat(changed.brokerType()).isEqualTo(BrokerType.PULSAR);
    }

    static MessageTemplate aTemplate(String name) {
        return MessageTemplate.create(
                TestIds.templateId(),
                new TemplateName(name),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders-{{env}}"),
                "{{orderId}}",
                Map.of("source", "topicman"),
                new TemplatePayload(PayloadFormat.JSON, "{\"id\": \"{{orderId}}\"}"));
    }
}
