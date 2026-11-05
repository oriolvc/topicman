package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.ConnectionTestOutcome;
import com.oriolvc.topicman.messaging.domain.port.ConnectionTester;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Tests a Pulsar endpoint with a topic lookup, the first thing producing and reading do. Automatic
 * topic creation is off for the lookup, so a "not found" answer proves the broker answers and no
 * topic is created.
 */
final class PulsarConnectionTester implements ConnectionTester {

    private static final String LOOKUP_TOPIC = "topicman-connection-test";

    private final PulsarClients clients;
    private final Duration openTimeout;
    private final Duration connectTimeout;

    PulsarConnectionTester(PulsarClients clients, PulsarProperties properties) {
        this.clients = clients;
        this.openTimeout = properties.openTimeout();
        this.connectTimeout = properties.connectTimeout();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.PULSAR;
    }

    @Override
    public ConnectionTestOutcome test(BrokerEndpoint endpoint) {
        org.apache.pulsar.client.api.PulsarClient client;
        try {
            client = clients.forEndpoint(endpoint);
        } catch (PulsarClientException e) {
            return ConnectionTestOutcome.failed("Invalid Pulsar service URL: " + e.getMessage());
        }
        var unreachable = PulsarServiceUrlProbe.unreachableMessage(endpoint.address(), connectTimeout);
        if (unreachable.isPresent()) {
            return ConnectionTestOutcome.failed(unreachable.get());
        }
        var lookup = client.getPartitionsForTopic(LOOKUP_TOPIC, false);
        try {
            lookup.get(openTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return answered(endpoint);
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof PulsarClientException.NotFoundException
                    || cause instanceof PulsarClientException.TopicDoesNotExistException) {
                return answered(endpoint);
            }
            return ConnectionTestOutcome.failed("The Pulsar broker refused a topic lookup: " + cause.getMessage());
        } catch (TimeoutException e) {
            lookup.cancel(true);
            return ConnectionTestOutcome.failed(
                    "The Pulsar broker did not answer a topic lookup within " + openTimeout.toSeconds() + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            lookup.cancel(true);
            return ConnectionTestOutcome.failed("Interrupted while testing the Pulsar connection");
        }
    }

    private static ConnectionTestOutcome answered(BrokerEndpoint endpoint) {
        return ConnectionTestOutcome.succeeded("Pulsar broker at " + endpoint.address() + " answered a topic lookup");
    }
}
