package com.oriolvc.topicman.messaging.domain;

import java.time.Duration;
import java.util.List;

/**
 * A live, pull-based read from a broker. Pulling keeps backpressure with the caller, which runs one
 * virtual thread per consume session. Not thread-safe: use it from a single thread.
 */
public interface MessageStream extends AutoCloseable {

    /**
     * Returns the messages that arrive within the timeout, possibly none. Returns as soon as at
     * least one message is available.
     */
    List<ReceivedMessage> poll(Duration timeout);

    /** Releases the broker resources (consumer, reader or temporary queue). */
    @Override
    void close();
}
