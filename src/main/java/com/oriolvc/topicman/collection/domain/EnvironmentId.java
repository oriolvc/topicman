package com.oriolvc.topicman.collection.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity of a environment. */
public record EnvironmentId(UUID value) {

    public EnvironmentId {
        Objects.requireNonNull(value, "value");
    }

    public static EnvironmentId newId() {
        return new EnvironmentId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
