package com.oriolvc.topicman.collection.domain;

import java.time.Instant;
import java.util.UUID;

/** Answers with fixed values and counts how often each was asked for. */
class FixedDynamicValues implements DynamicValues {

    static final UUID UUID_VALUE = UUID.fromString("0b6c1f1d-2a10-4a55-9a43-7f0c3c1e5b8e");
    static final Instant NOW = Instant.parse("2026-11-03T10:15:30.123456Z");
    static final int RANDOM_INT = 7;

    int uuids;
    int nows;
    int randomInts;

    @Override
    public UUID randomUuid() {
        uuids++;
        return UUID_VALUE;
    }

    @Override
    public Instant now() {
        nows++;
        return NOW;
    }

    @Override
    public int randomInt() {
        randomInts++;
        return RANDOM_INT;
    }
}
