package com.oriolvc.topicman.messaging.application;

import java.time.Duration;
import java.util.Objects;

/**
 * @param maxOpen how many sessions can be open at once; each holds a broker client
 * @param idleTimeout how long a session stays open while no client streams it
 */
public record ConsumeSessionLimits(int maxOpen, Duration idleTimeout) {

    public ConsumeSessionLimits {
        Objects.requireNonNull(idleTimeout, "idleTimeout");
        if (maxOpen < 1) {
            throw new IllegalArgumentException("maxOpen must be at least 1");
        }
        if (idleTimeout.isNegative() || idleTimeout.isZero()) {
            throw new IllegalArgumentException("idleTimeout must be positive");
        }
    }
}
