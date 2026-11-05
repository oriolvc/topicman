package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.MessageTemplate;
import com.oriolvc.topicman.collection.domain.model.TemplateDestination;
import com.oriolvc.topicman.collection.domain.model.TemplateId;
import com.oriolvc.topicman.collection.domain.model.TemplateName;
import com.oriolvc.topicman.collection.domain.model.TemplatePayload;
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

    public MessageTemplate toNewTemplate(TemplateId id) {
        return MessageTemplate.create(id, name, brokerType, destination, key, headers, payload);
    }

    public MessageTemplate applyTo(MessageTemplate template) {
        return template.withContent(name, brokerType, destination, key, headers, payload);
    }
}
