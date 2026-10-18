package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

public final class ConsumeSessionNotFoundException extends DomainException {

    public static final String CODE = "messaging.session-not-found";

    public ConsumeSessionNotFoundException(ConsumeSessionId id) {
        super(CODE, "No open consume session has id " + id);
    }
}
