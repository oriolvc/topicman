package com.oriolvc.topicman.messaging.domain.model;

import com.oriolvc.topicman.messaging.domain.exception.InvalidMessageException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Message headers (Kafka headers, Pulsar properties, AMQP headers), as an immutable, ordered copy.
 * Names must not be blank; values may be empty.
 */
public record Headers(Map<String, String> values) {

    private static final Headers NONE = new Headers(Map.of());

    public Headers {
        Objects.requireNonNull(values, "values");
        values.forEach((name, value) -> {
            if (name == null || name.isBlank()) {
                throw new InvalidMessageException("Header names must not be blank");
            }
            if (value == null) {
                throw new InvalidMessageException("Header '" + name + "' must have a value");
            }
        });
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static Headers of(Map<String, String> values) {
        return new Headers(values);
    }

    public static Headers none() {
        return NONE;
    }
}
