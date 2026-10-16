package com.oriolvc.topicman.shared.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a broker connection. Lives in the shared kernel so other contexts can reference a
 * connection without depending on the connection context's internals.
 */
public record ConnectionId(UUID value) {

    public ConnectionId {
        Objects.requireNonNull(value, "value");
    }

    public static ConnectionId newId() {
        return new ConnectionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
