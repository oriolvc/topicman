package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

public final class ConsumeSessionClosedException extends DomainException {

    public static final String CODE = "messaging.consume-session-closed";

    ConsumeSessionClosedException(ConsumeSessionId id) {
        super(CODE, "Consume session " + id + " is closed");
    }
}
