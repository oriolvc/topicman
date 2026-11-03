package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.ConnectionTestOutcome;
import com.oriolvc.topicman.messaging.domain.ConnectionTester;
import com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq.RabbitMqConnections.ConnectionFailedException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests a RabbitMQ endpoint by opening a new AMQP connection, which covers TCP, the handshake,
 * authentication and the virtual host, and closing it again. The shared connection is not used: an
 * open one would prove nothing about the credentials as they are now.
 */
final class RabbitMqConnectionTester implements ConnectionTester {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConnectionTester.class);

    private final RabbitMqConnections connections;

    RabbitMqConnectionTester(RabbitMqConnections connections) {
        this.connections = connections;
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.RABBITMQ;
    }

    @Override
    public ConnectionTestOutcome test(BrokerEndpoint endpoint) {
        com.rabbitmq.client.Connection connection;
        try {
            connection = connections.openNew(endpoint);
        } catch (ConnectionFailedException e) {
            return ConnectionTestOutcome.failed(String.valueOf(e.getMessage()));
        }
        try {
            var version = connection.getServerProperties().get("version");
            return ConnectionTestOutcome.succeeded("RabbitMQ " + version + " at "
                    + connection.getAddress().getHostAddress() + ":" + connection.getPort());
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
                log.debug("Could not close the RabbitMQ test connection cleanly", e);
            }
        }
    }
}
