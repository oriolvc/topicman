package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConnectionTestOutcome;
import java.time.Duration;
import java.util.Objects;

/** @param duration how long the test took */
public record TestConnectionResult(ConnectionTestOutcome outcome, Duration duration) {

    public TestConnectionResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(duration, "duration");
    }
}
