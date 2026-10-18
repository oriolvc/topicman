package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** Connection names are unique, so users can tell connections apart. */
public final class ConnectionNameTakenException extends DomainException {

    public static final String CODE = "connection.name-taken";

    public ConnectionNameTakenException(ConnectionName name) {
        super(CODE, "Another connection is already named '" + name.value() + "'");
    }
}
