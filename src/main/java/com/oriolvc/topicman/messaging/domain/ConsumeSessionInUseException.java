package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** One client streams a session at a time, because a broker stream can only be read by one thread. */
public final class ConsumeSessionInUseException extends DomainException {

    public static final String CODE = "messaging.session-in-use";

    public ConsumeSessionInUseException(ConsumeSessionId id) {
        super(CODE, "Another client is already streaming consume session " + id);
    }
}
