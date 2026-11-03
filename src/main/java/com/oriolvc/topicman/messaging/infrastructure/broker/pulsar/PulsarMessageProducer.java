package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

/**
 * Produces to Pulsar and waits for the broker to persist each message. A Pulsar producer is bound
 * to one topic, so one is kept per endpoint and topic: producing N copies then pays the topic
 * lookup once. Batching is off, since a UI sends one message at a time. A producer is discarded
 * after a failed send, so a broken one is not reused. The service URL is probed before a producer is
 * created, so an unreachable broker fails at once instead of at the send timeout.
 */
final class PulsarMessageProducer implements MessageProducer, AutoCloseable {

    private record ProducerKey(BrokerEndpoint endpoint, String topic) {}

    private final PulsarClients clients;
    private final Duration sendTimeout;
    private final Duration connectTimeout;
    private final Map<ProducerKey, Producer<byte[]>> producers = new ConcurrentHashMap<>();

    PulsarMessageProducer(PulsarClients clients, PulsarProperties properties) {
        this.clients = clients;
        this.sendTimeout = properties.sendTimeout();
        this.connectTimeout = properties.connectTimeout();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.PULSAR;
    }

    @Override
    public SendReceipt send(BrokerEndpoint endpoint, Destination destination, Message message) {
        if (!(destination instanceof Destination.Topic(var topic))) {
            throw new InvalidDestinationException("Pulsar produces to topics, not to " + destination);
        }
        var key = new ProducerKey(endpoint, topic);
        Producer<byte[]> producer;
        try {
            producer = producerFor(key);
        } catch (PulsarClientException e) {
            throw new ProduceFailedException(failureMessage(topic, e), e);
        }
        try {
            var builder = producer.newMessage()
                    .value(message.payload().bytes())
                    .properties(message.headers().values());
            if (message.key() != null) {
                builder.key(message.key());
            }
            return new SendReceipt(builder.send().toString());
        } catch (PulsarClientException e) {
            discard(key, producer);
            throw new ProduceFailedException(failureMessage(topic, e), e);
        }
    }

    @Override
    public void close() {
        producers.forEach(this::discard);
    }

    private Producer<byte[]> producerFor(ProducerKey key) throws PulsarClientException {
        var existing = producers.get(key);
        if (existing != null) {
            return existing;
        }
        var client = clients.forEndpoint(key.endpoint());
        var unreachable =
                PulsarServiceUrlProbe.unreachableMessage(key.endpoint().address(), connectTimeout);
        if (unreachable.isPresent()) {
            throw new ProduceFailedException(unreachable.get());
        }
        var created = client.newProducer(Schema.BYTES)
                .topic(key.topic())
                .enableBatching(false)
                .sendTimeout((int) sendTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .create();
        var raced = producers.putIfAbsent(key, created);
        if (raced != null) {
            created.closeAsync();
            return raced;
        }
        return created;
    }

    private void discard(ProducerKey key, Producer<byte[]> producer) {
        producers.remove(key, producer);
        producer.closeAsync();
    }

    private static String failureMessage(String topic, PulsarClientException cause) {
        return "Could not produce to topic '" + topic + "': " + cause.getMessage();
    }
}
