package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

public final class InvalidMessageException extends DomainException {

    public static final String CODE = "messaging.invalid-message";

    public InvalidMessageException(String message) {
        super(CODE, message);
    }
}
