package com.oriolvc.topicman.messaging.application.helper;

import com.oriolvc.topicman.messaging.domain.model.ConsumeSession;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.port.MessageStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * An open consume session and its broker stream. A broker stream is not thread-safe, so it is used
 * by one thread at a time: the streaming client's while one is connected, otherwise the closer's.
 * The flags below decide which thread closes it, so it is never closed while being polled. A new
 * client takes the stream over by claiming it: the streaming thread notices that its claim is no
 * longer the latest, stops and releases the stream to it.
 */
public final class ActiveSession {

    private final ConsumeSession session;
    private final MessageStream stream;
    // Read but not delivered when the last client left; only touched by the streaming thread.
    private final Deque<ReceivedMessage> undelivered = new ArrayDeque<>();
    private boolean streaming;
    private boolean closeRequested;
    // Counts the claims; only the latest may stream.
    private long claims;

    public ActiveSession(ConsumeSession session, MessageStream stream) {
        this.session = session;
        this.stream = stream;
    }

    public ConsumeSession session() {
        return session;
    }

    public MessageStream stream() {
        return stream;
    }

    public Deque<ReceivedMessage> undelivered() {
        return undelivered;
    }

    /**
     * Claims the stream for a new client. The client streaming it, if any, is asked to stop and given
     * {@code handover} to release it; a later claim made meanwhile wins over this one.
     *
     * @return the claim, to check with {@link #isClaimedBy}; empty if the session closed, the stream
     *     was not released in time or a later claim won
     */
    public synchronized OptionalLong claim(Duration handover, Instant now) {
        var claim = ++claims;
        // Wakes an earlier claim still waiting, so it gives up.
        notifyAll();
        var deadline = System.nanoTime() + handover.toNanos();
        try {
            while (streaming && !closeRequested && claims == claim) {
                var remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return OptionalLong.empty();
                }
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OptionalLong.empty();
        }
        if (closeRequested || claims != claim) {
            return OptionalLong.empty();
        }
        streaming = true;
        recordActivity(now);
        return OptionalLong.of(claim);
    }

    /** Whether {@code claim} is still the latest, so its client may keep streaming. */
    public synchronized boolean isClaimedBy(long claim) {
        return claims == claim;
    }

    public synchronized boolean isCloseRequested() {
        return closeRequested;
    }

    /** @return whether the caller must close the stream, because no client is streaming it */
    public synchronized boolean requestClose() {
        closeRequested = true;
        notifyAll();
        return !streaming;
    }

    /** Called by the streaming thread when it stops. @return whether it must close the stream */
    public synchronized boolean release(Instant now) {
        streaming = false;
        // The idle timeout counts from when the last client left.
        recordActivity(now);
        notifyAll();
        return closeRequested;
    }

    /**
     * Marks the session for closing if no client streams it and it has been idle for the timeout,
     * under the same lock as {@link #claim}, so a client cannot claim it meanwhile.
     *
     * @return whether it expired; the caller then closes the stream
     */
    public synchronized boolean expireIfIdle(Duration timeout, Instant now) {
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
