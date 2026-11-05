package com.oriolvc.topicman.messaging.domain.port;

import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.ConnectionTestOutcome;
import com.oriolvc.topicman.shared.domain.BrokerType;

/**
 * Outbound port: checks that an endpoint reaches its broker the way producing and consuming will,
 * without producing, consuming or creating anything. Each broker adapter implements it.
 */
public interface ConnectionTester {

    /** The broker this tester talks to; the application layer picks the tester by it. */
    BrokerType brokerType();

    /**
     * Runs the test within the adapter's timeouts. A broker that cannot be reached is a failed
     * outcome, not an exception.
     */
    ConnectionTestOutcome test(BrokerEndpoint endpoint);
}
