package com.oriolvc.topicman.collection.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity of a message template. */
public record TemplateId(UUID value) {

    public TemplateId {
        Objects.requireNonNull(value, "value");
    }

    public static TemplateId newId() {
        return new TemplateId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
