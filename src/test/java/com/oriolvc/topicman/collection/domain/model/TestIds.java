package com.oriolvc.topicman.collection.domain.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Unique collection, template and environment ids for tests. They start far above what a test
 * database's sequences reach, so they never collide with ids a repository gave.
 */
public final class TestIds {

    private static final AtomicLong NEXT = new AtomicLong(1_000_000);

    private TestIds() {}

    public static CollectionId collectionId() {
        return new CollectionId(NEXT.getAndIncrement());
    }

    public static TemplateId templateId() {
        return new TemplateId(NEXT.getAndIncrement());
    }

    public static EnvironmentId environmentId() {
        return new EnvironmentId(NEXT.getAndIncrement());
    }
}
