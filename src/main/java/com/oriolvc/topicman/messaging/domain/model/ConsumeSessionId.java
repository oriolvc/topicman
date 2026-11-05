package com.oriolvc.topicman.messaging.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ConsumeSessionId(UUID value) {

    public ConsumeSessionId {
        Objects.requireNonNull(value, "value");
    }

    public static ConsumeSessionId newId() {
        return new ConsumeSessionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
