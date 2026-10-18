package com.oriolvc.topicman.messaging.infrastructure.connection;

import com.oriolvc.topicman.connection.application.GetConnectionCommand;
import com.oriolvc.topicman.connection.application.GetConnectionUseCase;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoints;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import org.springframework.stereotype.Component;

/**
 * Resolves endpoints through the connection context's application API, never its domain or
 * persistence. Credentials are added with the TLS and SASL task (Phase 5).
 */
@Component
class ConnectionBrokerEndpoints implements BrokerEndpoints {

    private final GetConnectionUseCase getConnection;

    ConnectionBrokerEndpoints(GetConnectionUseCase getConnection) {
        this.getConnection = getConnection;
    }

    @Override
    public BrokerEndpoint of(ConnectionId connectionId) {
        var connection = getConnection.execute(new GetConnectionCommand(connectionId));
        return new BrokerEndpoint(connection.brokerType(), connection.address());
    }
}
