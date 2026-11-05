package com.oriolvc.topicman.messaging.infrastructure.connection;

import com.oriolvc.topicman.connection.application.model.BrokerCredentials;
import com.oriolvc.topicman.connection.application.model.GetBrokerAccessCommand;
import com.oriolvc.topicman.connection.application.usecase.GetBrokerAccessUseCase;
import com.oriolvc.topicman.messaging.domain.model.Authentication;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.Tls;
import com.oriolvc.topicman.messaging.domain.port.BrokerEndpoints;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import org.springframework.stereotype.Component;

/**
 * Resolves endpoints, credentials and TLS settings included, through the connection context's
 * application API, never its domain or persistence (ADR 0002, ADR 0008).
 */
@Component
class ConnectionBrokerEndpoints implements BrokerEndpoints {

    private final GetBrokerAccessUseCase getBrokerAccess;

    ConnectionBrokerEndpoints(GetBrokerAccessUseCase getBrokerAccess) {
        this.getBrokerAccess = getBrokerAccess;
    }

    @Override
    public BrokerEndpoint of(ConnectionId connectionId) {
        var access = getBrokerAccess.execute(new GetBrokerAccessCommand(connectionId));
        return new BrokerEndpoint(
                access.brokerType(),
                access.address(),
                toAuthentication(access.credentials()),
                new Tls(access.tlsEnabled(), access.trustedCertificates()));
    }

    private static Authentication toAuthentication(BrokerCredentials credentials) {
        return switch (credentials) {
            case BrokerCredentials.None none -> Authentication.none();
            case BrokerCredentials.Token token -> new Authentication.Token(token.token());
            case BrokerCredentials.UsernamePassword usernamePassword ->
                new Authentication.UsernamePassword(
                        usernamePassword.username(), usernamePassword.password(), usernamePassword.mechanism());
        };
    }
}
