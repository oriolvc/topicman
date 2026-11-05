package com.oriolvc.topicman.connection.domain.exception;

import com.oriolvc.topicman.shared.domain.DomainException;

public final class InvalidConnectionException extends DomainException {

    public static final String CODE = "connection.invalid-connection";

    public InvalidConnectionException(String message) {
        super(CODE, message);
    }
}
