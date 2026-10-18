package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.ArrayList;
import java.util.List;

/** Records what it sends; can be told to fail once a number of messages were acknowledged. */
class FakeMessageProducer implements MessageProducer {

    record Sent(BrokerEndpoint endpoint, Destination destination, Message message) {}

    private final BrokerType brokerType;
    private final List<Sent> sent = new ArrayList<>();
    private int failAfter = Integer.MAX_VALUE;

    FakeMessageProducer(BrokerType brokerType) {
        this.brokerType = brokerType;
    }

    void failAfter(int acknowledged) {
        failAfter = acknowledged;
    }

    List<Sent> sent() {
        return sent;
    }

    @Override
    public BrokerType brokerType() {
        return brokerType;
    }

    @Override
    public SendReceipt send(BrokerEndpoint endpoint, Destination destination, Message message) {
        if (sent.size() >= failAfter) {
            throw new ProduceFailedException("Broker unavailable", new RuntimeException("timeout"));
        }
        sent.add(new Sent(endpoint, destination, message));
        return new SendReceipt("receipt-" + sent.size());
    }
}
