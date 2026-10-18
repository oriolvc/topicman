package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.DomainException;

public final class ConnectionNotFoundException extends DomainException {

    public static final String CODE = "connection.not-found";

    public ConnectionNotFoundException(ConnectionId id) {
        super(CODE, "No connection has id " + id);
    }
}
