package com.oriolvc.topicman.collection.domain;

import java.util.Objects;
import java.util.UUID;

/** Identity of a collection. */
public record CollectionId(UUID value) {

    public CollectionId {
        Objects.requireNonNull(value, "value");
    }

    public static CollectionId newId() {
        return new CollectionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
