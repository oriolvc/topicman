package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

public final class InvalidDestinationException extends DomainException {

    public static final String CODE = "messaging.invalid-destination";

    public InvalidDestinationException(String message) {
        super(CODE, message);
    }
}
