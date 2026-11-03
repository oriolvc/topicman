package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One client per endpoint, shared by the producer and the reader. A Pulsar client owns I/O threads
 * and a connection pool, so it is created once and reconnects by itself; producers and readers on
 * top of it are cheap.
 */
final class PulsarClients implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PulsarClients.class);

    private final PulsarProperties properties;
    private final Map<BrokerEndpoint, PulsarClient> clients = new ConcurrentHashMap<>();

    PulsarClients(PulsarProperties properties) {
        this.properties = properties;
    }

    /**
     * @throws PulsarClientException if the service URL is invalid
     */
    PulsarClient forEndpoint(BrokerEndpoint endpoint) throws PulsarClientException {
        var existing = clients.get(endpoint);
        if (existing != null) {
            return existing;
        }
        var created = create(endpoint);
        var raced = clients.putIfAbsent(endpoint, created);
        if (raced != null) {
            closeQuietly(created);
            return raced;
        }
        return created;
    }

    @Override
    public void close() {
        clients.values().forEach(PulsarClients::closeQuietly);
        clients.clear();
    }

    private PulsarClient create(BrokerEndpoint endpoint) throws PulsarClientException {
        var operationTimeout = properties.sendTimeout().compareTo(properties.openTimeout()) > 0
                ? properties.sendTimeout()
                : properties.openTimeout();
        return PulsarClient.builder()
                .serviceUrl(endpoint.address())
                .connectionTimeout((int) properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .operationTimeout((int) operationTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    private static void closeQuietly(PulsarClient client) {
        try {
            client.close();
        } catch (PulsarClientException e) {
            log.debug("Could not close a Pulsar client cleanly", e);
        }
    }
}
