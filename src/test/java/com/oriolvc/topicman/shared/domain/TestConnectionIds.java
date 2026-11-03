package com.oriolvc.topicman.shared.domain;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Unique connection ids for tests. They start far above what a test database's sequence reaches,
 * so they never collide with ids a repository gave.
 */
public final class TestConnectionIds {

    private static final AtomicLong NEXT = new AtomicLong(1_000_000);

    private TestConnectionIds() {}

    public static ConnectionId next() {
        return new ConnectionId(NEXT.getAndIncrement());
    }
}
