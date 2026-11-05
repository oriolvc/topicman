package com.oriolvc.topicman.collection.domain.model;

/** Identity of a environment: a value of its table's sequence, given by the repository (ADR 0007). */
public record EnvironmentId(long value) {

    public EnvironmentId {
        if (value < 1) {
            throw new IllegalArgumentException("Environment id must be positive, not " + value);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
