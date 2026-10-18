package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConsumeSession;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An open consume session and its broker stream. A broker stream is not thread-safe, so it is used
 * by one thread at a time: the streaming client's while one is connected, otherwise the closer's.
 * The flags below decide which thread closes it, so it is never closed while being polled.
 */
final class ActiveSession {

    private final ConsumeSession session;
    private final MessageStream stream;
    // Read but not delivered when the last client left; only touched by the streaming thread.
    private final Deque<ReceivedMessage> undelivered = new ArrayDeque<>();
    private boolean streaming;
    private boolean closeRequested;

    ActiveSession(ConsumeSession session, MessageStream stream) {
        this.session = session;
        this.stream = stream;
    }

    ConsumeSession session() {
        return session;
    }

    MessageStream stream() {
        return stream;
    }

    Deque<ReceivedMessage> undelivered() {
        return undelivered;
    }

    synchronized boolean tryClaim(Instant now) {
        if (streaming || closeRequested) {
            return false;
        }
        streaming = true;
        recordActivity(now);
        return true;
    }

    synchronized boolean isCloseRequested() {
        return closeRequested;
    }

    /** @return whether the caller must close the stream, because no client is streaming it */
    synchronized boolean requestClose() {
        closeRequested = true;
        return !streaming;
    }

    /** Called by the streaming thread when it stops. @return whether it must close the stream */
    synchronized boolean release(Instant now) {
        streaming = false;
        // The idle timeout counts from when the last client left.
        recordActivity(now);
        return closeRequested;
    }

    /**
     * Marks the session for closing if no client streams it and it has been idle for the timeout,
     * under the same lock as {@link #tryClaim}, so a client cannot claim it meanwhile.
     *
     * @return whether it expired; the caller then closes the stream
     */
    synchronized boolean expireIfIdle(Duration timeout, Instant now) {
        if (streaming || closeRequested || !session.isIdle(timeout, now)) {
            return false;
        }
        closeRequested = true;
        return true;
    }

    private void recordActivity(Instant now) {
        if (session.isOpen()) {
            session.recordActivity(now);
        }
    }
}
