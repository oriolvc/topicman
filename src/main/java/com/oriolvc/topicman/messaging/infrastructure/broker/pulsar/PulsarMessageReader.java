package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

/**
 * Reads Pulsar topics without affecting real consumers (ADR 0003): the Reader API uses a
 * non-durable subscription that disappears when the reader closes. Topics are never created: the
 * topic is looked up with auto-creation off before the reader, which would create it, is opened.
 * The service URL is probed first, so an unreachable broker fails at once instead of at the open
 * timeout.
 */
final class PulsarMessageReader implements MessageReader {

    private final PulsarClients clients;
    private final Duration openTimeout;
    private final Duration connectTimeout;

    PulsarMessageReader(PulsarClients clients, PulsarProperties properties) {
        this.clients = clients;
        this.openTimeout = properties.openTimeout();
        this.connectTimeout = properties.connectTimeout();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.PULSAR;
    }

    @Override
    public MessageStream open(BrokerEndpoint endpoint, Destination destination, StartPosition startPosition) {
        if (!(destination instanceof Destination.Topic(var topic))) {
            throw new InvalidDestinationException("Pulsar reads from topics, not from " + destination);
        }
        try {
            var client = clients.forEndpoint(endpoint);
            var unreachable = PulsarServiceUrlProbe.unreachableMessage(endpoint.address(), connectTimeout);
            if (unreachable.isPresent()) {
                throw new ReadFailedException(unreachable.get());
            }
            await(client.getPartitionsForTopic(topic, false), topic);
            var reader = await(
                    client.newReader(Schema.BYTES)
                            .topic(topic)
                            .startMessageId(startMessageId(startPosition))
                            .createAsync(),
                    topic);
            return new PulsarMessageStream(reader, topic);
        } catch (PulsarClientException e) {
            throw new ReadFailedException(failureMessage(topic, e), e);
        }
    }

    private <T> T await(CompletableFuture<T> future, String topic) {
        try {
            return future.get(openTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new ReadFailedException("Interrupted while opening topic '" + topic + "'", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ReadFailedException(
                    "Could not read from topic '" + topic + "': no answer within " + openTimeout.toSeconds() + "s", e);
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof PulsarClientException.NotFoundException
                    || cause instanceof PulsarClientException.TopicDoesNotExistException) {
                throw new ReadFailedException("Topic '" + topic + "' does not exist", cause);
            }
            throw new ReadFailedException(failureMessage(topic, cause), cause);
        }
    }

    private static MessageId startMessageId(StartPosition startPosition) {
        return switch (startPosition) {
            case EARLIEST -> MessageId.earliest;
            case LATEST -> MessageId.latest;
        };
    }

    private static String failureMessage(String topic, Throwable cause) {
        return "Could not read from topic '" + topic + "': " + cause.getMessage();
    }
}
