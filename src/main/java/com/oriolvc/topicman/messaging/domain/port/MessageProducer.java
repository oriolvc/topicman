package com.oriolvc.topicman.messaging.domain.port;

import com.oriolvc.topicman.messaging.domain.exception.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.exception.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.Message;
import com.oriolvc.topicman.messaging.domain.model.SendReceipt;
import com.oriolvc.topicman.shared.domain.BrokerType;

/** Outbound port: produces messages to one kind of broker. Each broker adapter implements it. */
public interface MessageProducer {

    /** The broker this producer talks to; the application layer picks the producer by it. */
    BrokerType brokerType();

    /**
     * Sends one message and waits for the broker's acknowledgement.
     *
     * @throws InvalidDestinationException if the destination is not supported by this broker
     * @throws ProduceFailedException if the broker rejects the message or does not acknowledge it in
     *     time
     */
    SendReceipt send(BrokerEndpoint endpoint, Destination destination, Message message);
}
