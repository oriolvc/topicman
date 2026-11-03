package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A template with every placeholder replaced, ready to produce through a connection of its broker
 * type. The texts are final; the messaging context still validates them when producing.
 *
 * @param key absent when the template has none or it resolved to nothing
 */
public record ResolvedMessage(
        BrokerType brokerType,
        TemplateDestination destination,
        @Nullable String key,
        Map<String, String> headers,
        TemplatePayload payload) {

    public ResolvedMessage {
        Objects.requireNonNull(brokerType, "brokerType");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(payload, "payload");
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
    }
}
