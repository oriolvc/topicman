package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;

/** Outbound port: where a saved connection's broker is, as an adapter needs it. */
public interface BrokerEndpoints {

    /** @throws ConnectionNotFoundException if no connection has this id */
    BrokerEndpoint of(ConnectionId connectionId);
}
