package com.oriolvc.topicman.messaging.application.usecase;

import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.port.BrokerEndpoints;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import java.util.HashMap;
import java.util.Map;

class InMemoryBrokerEndpoints implements BrokerEndpoints {

    private final Map<ConnectionId, BrokerEndpoint> endpoints = new HashMap<>();

    ConnectionId add(BrokerType type, String address) {
        var id = TestConnectionIds.next();
        endpoints.put(id, new BrokerEndpoint(type, address));
        return id;
    }

    @Override
    public BrokerEndpoint of(ConnectionId connectionId) {
        var endpoint = endpoints.get(connectionId);
        if (endpoint == null) {
            throw new ConnectionNotFoundException(connectionId);
        }
        return endpoint;
    }
}
