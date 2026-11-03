package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.MessageTemplate;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Everything a client sets on a template; its id is kept by the collection. */
public record TemplateContent(
        TemplateName name,
        BrokerType brokerType,
        TemplateDestination destination,
        @Nullable String key,
        Map<String, String> headers,
        TemplatePayload payload) {

    MessageTemplate toNewTemplate() {
        return MessageTemplate.create(name, brokerType, destination, key, headers, payload);
    }

    MessageTemplate applyTo(MessageTemplate template) {
        return template.withContent(name, brokerType, destination, key, headers, payload);
    }
}
