package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.ConnectionTestOutcome;
import com.oriolvc.topicman.messaging.domain.ConnectionTester;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.ArrayList;
import java.util.List;

/** Answers every test with a set outcome and records the endpoints it tested. */
class FakeConnectionTester implements ConnectionTester {

    private final BrokerType brokerType;
    private final List<BrokerEndpoint> tested = new ArrayList<>();
    private ConnectionTestOutcome outcome = ConnectionTestOutcome.succeeded("Broker answered");

    FakeConnectionTester(BrokerType brokerType) {
        this.brokerType = brokerType;
    }

    void answer(ConnectionTestOutcome outcome) {
        this.outcome = outcome;
    }

    List<BrokerEndpoint> tested() {
        return tested;
    }

    @Override
    public BrokerType brokerType() {
        return brokerType;
    }

    @Override
    public ConnectionTestOutcome test(BrokerEndpoint endpoint) {
        tested.add(endpoint);
        return outcome;
    }
}
