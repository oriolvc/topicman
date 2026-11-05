package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.TemplateContent;
import com.oriolvc.topicman.collection.domain.model.PayloadFormat;
import com.oriolvc.topicman.collection.domain.model.TemplateDestination;
import com.oriolvc.topicman.collection.domain.model.TemplateName;
import com.oriolvc.topicman.collection.domain.model.TemplatePayload;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

final class CollectionFixtures {

    static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private CollectionFixtures() {}

    static TemplateContent aKafkaTemplate(String name) {
        return new TemplateContent(
                new TemplateName(name),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders-{{env}}"),
                "{{orderId}}",
                Map.of("source", "topicman"),
                new TemplatePayload(PayloadFormat.JSON, "{\"id\": \"{{orderId}}\", \"trace\": \"{{$uuid}}\"}"));
    }
}
