package com.oriolvc.topicman.shared.domain;

/**
 * Identity of a broker connection: a value of its table's sequence (ADR 0007). Lives in the shared
 * kernel so other contexts can reference a connection without depending on the connection
 * context's internals.
 */
public record ConnectionId(long value) {

    public ConnectionId {
        if (value < 1) {
            throw new IllegalArgumentException("Connection id must be positive, not " + value);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
