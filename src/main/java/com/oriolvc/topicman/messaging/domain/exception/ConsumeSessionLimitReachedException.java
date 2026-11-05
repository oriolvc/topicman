package com.oriolvc.topicman.messaging.domain.exception;

import com.oriolvc.topicman.shared.domain.DomainException;

/** Every open session holds a broker client, so their number is capped. */
public final class ConsumeSessionLimitReachedException extends DomainException {

    public static final String CODE = "messaging.session-limit-reached";

    public ConsumeSessionLimitReachedException(int limit) {
        super(CODE, "At most " + limit + " consume sessions can be open; close one first");
    }
}
