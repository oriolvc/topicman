package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One AMQP connection per endpoint, shared by the producer and the reader, which each work on
 * their own channels. Automatic recovery is off: a dropped connection fails the sessions on it,
 * and the next use opens a new one.
 *
 * <p>An AMQP URI may carry a username and password, so failure messages name only the host and
 * port, never the URI.
 */
final class RabbitMqConnections implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConnections.class);

    private final RabbitMqProperties properties;
    private final Map<BrokerEndpoint, Connection> connections = new ConcurrentHashMap<>();

    RabbitMqConnections(RabbitMqProperties properties) {
        this.properties = properties;
    }

    /** An open connection to the endpoint, created on first use and after the previous one closed. */
    Connection forEndpoint(BrokerEndpoint endpoint) throws ConnectionFailedException {
        try {
            // Atomic per endpoint, so concurrent first uses open a single connection.
            return connections.compute(
                    endpoint, (key, existing) -> existing != null && existing.isOpen() ? existing : openUnchecked(key));
        } catch (UncheckedConnectionFailure e) {
            throw e.getCause();
        }
    }

    @Override
    public void close() {
        connections.values().forEach(RabbitMqConnections::closeQuietly);
        connections.clear();
    }

    private Connection openUnchecked(BrokerEndpoint endpoint) {
        try {
            return openNew(endpoint);
        } catch (ConnectionFailedException e) {
            throw new UncheckedConnectionFailure(e);
        }
    }

    /** A new connection that the caller owns and closes; not shared. */
    Connection openNew(BrokerEndpoint endpoint) throws ConnectionFailedException {
        var factory = new ConnectionFactory();
        try {
            factory.setUri(endpoint.address());
        } catch (Exception e) {
            // The exception may quote the URI, credentials included, so it is neither kept nor shown.
            throw new ConnectionFailedException("Invalid AMQP URI; expected amqp://host:port or amqps://host:port");
        }
        factory.setAutomaticRecoveryEnabled(false);
        factory.setTopologyRecoveryEnabled(false);
        factory.setConnectionTimeout((int) properties.connectTimeout().toMillis());
        factory.setHandshakeTimeout((int) properties.openTimeout().toMillis());
        factory.setChannelRpcTimeout((int) properties.openTimeout().toMillis());
        var server = factory.getHost() + ":" + factory.getPort();
        try {
            return factory.newConnection("topicman");
        } catch (ConnectException | NoRouteToHostException | UnknownHostException | SocketTimeoutException e) {
            throw new ConnectionFailedException(
                    "Cannot reach the RabbitMQ broker at " + server + ": " + e.getMessage());
        } catch (IOException | TimeoutException e) {
            throw new ConnectionFailedException(
                    "Could not connect to the RabbitMQ broker at " + server + ": " + e.getMessage());
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (Exception e) {
            log.debug("Could not close a RabbitMQ connection cleanly", e);
        }
    }

    /** Opening a connection failed; the message is safe to show and holds no credentials. */
    static final class ConnectionFailedException extends Exception {

        ConnectionFailedException(String message) {
            super(message);
        }
    }

    /** Carries a {@link ConnectionFailedException} out of the {@code compute} function. */
    private static final class UncheckedConnectionFailure extends RuntimeException {

        UncheckedConnectionFailure(ConnectionFailedException cause) {
            super(cause);
        }

        @Override
        public synchronized ConnectionFailedException getCause() {
            return (ConnectionFailedException) super.getCause();
        }
    }
}
