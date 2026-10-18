package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;

/**
 * Outbound port: reads messages from one kind of broker without side effects (ADR 0003). Each
 * broker adapter implements it.
 */
public interface MessageReader {

    /** The broker this reader talks to; the application layer picks the reader by it. */
    BrokerType brokerType();

    /**
     * Starts reading from the destination. The caller owns the returned stream and must close it.
     *
     * @throws InvalidDestinationException if the destination is not supported by this broker
     * @throws ReadFailedException if the destination does not exist or the broker cannot be reached
     */
    MessageStream open(BrokerEndpoint endpoint, Destination destination, StartPosition startPosition);
}
