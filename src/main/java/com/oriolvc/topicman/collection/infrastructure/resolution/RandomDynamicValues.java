package com.oriolvc.topicman.collection.infrastructure.resolution;

import com.oriolvc.topicman.collection.domain.DynamicValues;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Random UUIDs and numbers, and the application clock's time, for the built-in variables. */
@Component
class RandomDynamicValues implements DynamicValues {

    private final Clock clock;

    RandomDynamicValues(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UUID randomUuid() {
        return UUID.randomUUID();
    }

    @Override
    public Instant now() {
        return clock.instant();
    }

    @Override
    public int randomInt() {
        return ThreadLocalRandom.current().nextInt(1001);
    }
}
