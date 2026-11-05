package com.oriolvc.topicman.collection.domain.model;

/** Identity of a message template: a value of its table's sequence, given by the repository (ADR 0007). */
public record TemplateId(long value) {

    public TemplateId {
        if (value < 1) {
            throw new IllegalArgumentException("Template id must be positive, not " + value);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
