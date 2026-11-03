package com.oriolvc.topicman.collection.domain;

/** Identity of a collection: a value of its table's sequence, given by the repository (ADR 0007). */
public record CollectionId(long value) {

    public CollectionId {
        if (value < 1) {
            throw new IllegalArgumentException("Collection id must be positive, not " + value);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
