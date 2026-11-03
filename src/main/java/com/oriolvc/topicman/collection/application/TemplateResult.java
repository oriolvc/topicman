package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.MessageTemplate;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record TemplateResult(
        UUID id,
        String name,
        BrokerType brokerType,
        TemplateDestination destination,
        @Nullable String key,
        Map<String, String> headers,
        TemplatePayload payload) {

    static TemplateResult of(MessageTemplate template) {
        return new TemplateResult(
                template.id().value(),
                template.name().value(),
                template.brokerType(),
                template.destination(),
                template.key(),
                template.headers(),
                template.payload());
    }
}
