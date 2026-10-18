package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/**
 * The broker rejected a message or did not acknowledge it in time. Adapters throw it instead of
 * their client's exceptions, so no broker client type leaves the adapter.
 */
public final class ProduceFailedException extends DomainException {

    public static final String CODE = "messaging.produce-failed";

    public ProduceFailedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
