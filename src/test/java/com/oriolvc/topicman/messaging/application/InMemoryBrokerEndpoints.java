package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoints;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import java.util.HashMap;
import java.util.Map;

class InMemoryBrokerEndpoints implements BrokerEndpoints {

    private final Map<ConnectionId, BrokerEndpoint> endpoints = new HashMap<>();

    ConnectionId add(BrokerType type, String address) {
        var id = ConnectionId.newId();
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
