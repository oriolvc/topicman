package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.domain.port.DynamicValues;
import java.time.Instant;
import java.util.UUID;

class FixedDynamicValues implements DynamicValues {

    static final UUID UUID_VALUE = UUID.fromString("0b6c1f1d-2a10-4a55-9a43-7f0c3c1e5b8e");

    @Override
    public UUID randomUuid() {
        return UUID_VALUE;
    }

    @Override
    public Instant now() {
        return Instant.parse("2026-11-03T10:15:30Z");
    }

    @Override
    public int randomInt() {
        return 7;
    }
}
