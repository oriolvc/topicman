package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

public final class InvalidBrokerEndpointException extends DomainException {

    public static final String CODE = "messaging.invalid-broker-endpoint";

    InvalidBrokerEndpointException(String message) {
        super(CODE, message);
    }
}
