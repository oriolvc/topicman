package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.MessageTemplate;
import com.oriolvc.topicman.collection.domain.model.TemplateDestination;
import com.oriolvc.topicman.collection.domain.model.TemplatePayload;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record TemplateResult(
        long id,
        String name,
        BrokerType brokerType,
        TemplateDestination destination,
        @Nullable String key,
        Map<String, String> headers,
        TemplatePayload payload) {

    public static TemplateResult of(MessageTemplate template) {
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
