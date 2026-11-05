package com.oriolvc.topicman.messaging.domain.port;

import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;
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
     *
     * @throws ReadFailedException if the broker connection is lost
     */
    List<ReceivedMessage> poll(Duration timeout);

    /** Releases the broker resources (consumer, reader or temporary queue). */
    @Override
    void close();
}
