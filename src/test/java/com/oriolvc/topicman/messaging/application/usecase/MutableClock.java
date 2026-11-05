package com.oriolvc.topicman.messaging.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests move forward, by hand or on every read. */
class MutableClock extends Clock {

    private Instant now;
    private Duration tick = Duration.ZERO;

    MutableClock(Instant start) {
        this.now = start;
    }

    void set(Instant now) {
        this.now = now;
    }

    void advanceOnEveryRead(Duration tick) {
        this.tick = tick;
    }

    @Override
    public Instant instant() {
        var current = now;
        now = now.plus(tick);
        return current;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
