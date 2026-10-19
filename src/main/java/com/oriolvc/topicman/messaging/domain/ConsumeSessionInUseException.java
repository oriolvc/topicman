package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/**
 * One client streams a session at a time, because a broker stream can only be read by one thread. A
 * new client takes the stream over; this is thrown when the previous one does not hand it over in
 * time.
 */
public final class ConsumeSessionInUseException extends DomainException {

    public static final String CODE = "messaging.session-in-use";

    public ConsumeSessionInUseException(ConsumeSessionId id) {
        super(CODE, "The client streaming consume session " + id + " did not hand it over in time");
    }
}
