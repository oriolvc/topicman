package com.oriolvc.topicman.shared.domain;

/** In the shared kernel, so every context that references a connection reports it the same way. */
public final class ConnectionNotFoundException extends DomainException {

    public static final String CODE = "connection.not-found";

    public ConnectionNotFoundException(ConnectionId id) {
        super(CODE, "No connection has id " + id);
    }
}
