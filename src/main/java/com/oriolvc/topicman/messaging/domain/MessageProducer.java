package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;

/** Outbound port: produces messages to one kind of broker. Each broker adapter implements it. */
public interface MessageProducer {

    /** The broker this producer talks to; the application layer picks the producer by it. */
    BrokerType brokerType();

    /**
     * Sends one message and waits for the broker's acknowledgement.
     *
     * @throws InvalidDestinationException if the destination is not supported by this broker
     */
    SendReceipt send(BrokerEndpoint endpoint, Destination destination, Message message);
}
