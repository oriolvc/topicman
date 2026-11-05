package com.oriolvc.topicman.messaging.application.model;

import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;

/**
 * Outbound port: the client a consume session streams to, such as a server-sent event stream.
 * Called from a single thread.
 */
public interface ConsumeSessionSink {

    /** Whether the client is still there. Checked on every poll, so a silent disconnect is noticed. */
    boolean isConnected();

    /** @return whether the message reached the client; if not, it is kept for the next client */
    boolean message(ReceivedMessage message);

    /** Keeps an idle connection open through proxies. @return whether it reached the client */
    boolean heartbeat();

    /** Reading failed; the session is closed and the stream ends after this. */
    void failed(ReadFailedException failure);

    /** The session was closed; the stream ends after this. */
    void closed();

    /** Another client took the stream over; the session stays open and this stream ends after this. */
    void replaced();
}
