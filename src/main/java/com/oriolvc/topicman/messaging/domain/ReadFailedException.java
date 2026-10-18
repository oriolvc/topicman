package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/**
 * A destination could not be opened or read, for example it does not exist or the broker cannot
 * be reached. Adapters throw it instead of their client's exceptions.
 */
public final class ReadFailedException extends DomainException {

    public static final String CODE = "messaging.read-failed";

    public ReadFailedException(String message) {
        super(CODE, message);
    }

    public ReadFailedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
