package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A saved message, with {@code {{variable}}} placeholders in any of its texts. It targets a broker
 * type rather than a connection, so it can be sent through any connection of that type and stays
 * portable when exported (ADR 0006). Immutable: a collection replaces a template to change it.
 *
 * @param key optional partitioning key; brokers without keys are rejected when the template is saved
 * @param headers header names and values, in order
 */
public record MessageTemplate(
        TemplateId id,
        TemplateName name,
        BrokerType brokerType,
        TemplateDestination destination,
        @Nullable String key,
        Map<String, String> headers,
        TemplatePayload payload) {

    public MessageTemplate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(brokerType, "brokerType");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(payload, "payload");
        if (key != null && key.isEmpty()) {
            throw new InvalidTemplateException("Template key must not be empty; omit it instead");
        }
        headers = copyOf(headers);
    }

    public static MessageTemplate create(
            TemplateName name,
            BrokerType brokerType,
            TemplateDestination destination,
            @Nullable String key,
            Map<String, String> headers,
            TemplatePayload payload) {
        return new MessageTemplate(TemplateId.newId(), name, brokerType, destination, key, headers, payload);
    }

    /** The same template, with every field but its id replaced. */
    public MessageTemplate withContent(
            TemplateName name,
            BrokerType brokerType,
            TemplateDestination destination,
            @Nullable String key,
            Map<String, String> headers,
            TemplatePayload payload) {
        return new MessageTemplate(id, name, brokerType, destination, key, headers, payload);
    }

    private static Map<String, String> copyOf(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers");
        var copy = new LinkedHashMap<String, String>();
        headers.forEach((name, value) -> {
            if (name == null || name.isBlank()) {
                throw new InvalidTemplateException("Header names must not be blank");
            }
            if (value == null) {
                throw new InvalidTemplateException("Header '" + name + "' must have a value");
            }
            copy.put(name, value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
