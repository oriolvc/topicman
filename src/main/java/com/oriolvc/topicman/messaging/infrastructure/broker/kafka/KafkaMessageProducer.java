package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Produces to Kafka and waits for the acknowledgement of all in-sync replicas. One producer is
 * kept per endpoint, because creating one fetches cluster metadata and producing N copies would
 * otherwise pay that N times. A producer is discarded after a failed send, so an endpoint that
 * stopped working does not keep a client retrying in the background. Before a producer is created,
 * the bootstrap servers are probed, so an unreachable broker fails at once instead of at the send
 * timeout.
 */
final class KafkaMessageProducer implements MessageProducer, AutoCloseable {

    private final Duration sendTimeout;
    private final Duration connectTimeout;
    private final Map<BrokerEndpoint, Producer<String, byte[]>> producers = new ConcurrentHashMap<>();

    KafkaMessageProducer(KafkaProperties properties) {
        this.sendTimeout = properties.sendTimeout();
        this.connectTimeout = properties.connectTimeout();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.KAFKA;
    }

    @Override
    public SendReceipt send(BrokerEndpoint endpoint, Destination destination, Message message) {
        if (!(destination instanceof Destination.Topic(var topic))) {
            throw new InvalidDestinationException("Kafka produces to topics, not to " + destination);
        }
        if (!producers.containsKey(endpoint)) {
            var unreachable = KafkaBootstrapProbe.unreachableMessage(endpoint.address(), connectTimeout);
            if (unreachable.isPresent()) {
                throw new ProduceFailedException(unreachable.get());
            }
        }
        Producer<String, byte[]> producer;
        try {
            producer = producers.computeIfAbsent(endpoint, this::createProducer);
        } catch (KafkaException e) {
            throw new ProduceFailedException(failureMessage(topic, e), e);
        }
        try {
            var metadata = producer.send(toRecord(topic, message))
                    // send() already bounds the wait through the timeouts set in createProducer
                    .get(sendTimeout.multipliedBy(2).toMillis(), TimeUnit.MILLISECONDS);
            return new SendReceipt(metadata.topic() + "-" + metadata.partition() + "@" + metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            discard(endpoint, producer);
            throw new ProduceFailedException("Interrupted while producing to topic '" + topic + "'", e);
        } catch (ExecutionException e) {
            discard(endpoint, producer);
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new ProduceFailedException(failureMessage(topic, cause), cause);
        } catch (KafkaException | TimeoutException e) {
            discard(endpoint, producer);
            throw new ProduceFailedException(failureMessage(topic, e), e);
        }
    }

    @Override
    public void close() {
        producers.forEach(this::discard);
    }

    private Producer<String, byte[]> createProducer(BrokerEndpoint endpoint) {
        var timeoutMs = (int) sendTimeout.toMillis();
        Map<String, Object> config = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                endpoint.address(),
                ProducerConfig.CLIENT_ID_CONFIG,
                KafkaClientIds.next("producer"),
                ProducerConfig.ACKS_CONFIG,
                "all",
                // One message at a time from a UI: send right away instead of batching.
                ProducerConfig.LINGER_MS_CONFIG,
                0,
                ProducerConfig.MAX_BLOCK_MS_CONFIG,
                timeoutMs,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                timeoutMs,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                timeoutMs);
        return new KafkaProducer<>(
                KafkaSecurity.withSecurity(endpoint, config), new StringSerializer(), new ByteArraySerializer());
    }

    private void discard(BrokerEndpoint endpoint, Producer<String, byte[]> producer) {
        producers.remove(endpoint, producer);
        producer.close(Duration.ZERO);
    }

    private static ProducerRecord<String, byte[]> toRecord(String topic, Message message) {
        var record =
                new ProducerRecord<>(topic, message.key(), message.payload().bytes());
        message.headers().values().forEach((name, value) -> record.headers().add(name, value.getBytes(UTF_8)));
        return record;
    }

    private static String failureMessage(String topic, Throwable cause) {
        return "Could not produce to topic '" + topic + "': " + cause.getMessage();
    }
}
