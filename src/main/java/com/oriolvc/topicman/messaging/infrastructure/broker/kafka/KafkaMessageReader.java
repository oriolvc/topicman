package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Reads Kafka topics without affecting real consumers (ADR 0003): no {@code group.id}, partitions
 * are assigned rather than subscribed, offsets are never committed and topics are never created.
 * The bootstrap servers are probed first, so an unreachable broker fails at once instead of at the
 * open timeout.
 */
final class KafkaMessageReader implements MessageReader {

    private final Duration openTimeout;
    private final Duration connectTimeout;

    KafkaMessageReader(KafkaProperties properties) {
        this.openTimeout = properties.openTimeout();
        this.connectTimeout = properties.connectTimeout();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.KAFKA;
    }

    @Override
    public MessageStream open(BrokerEndpoint endpoint, Destination destination, StartPosition startPosition) {
        if (!(destination instanceof Destination.Topic(var topic))) {
            throw new InvalidDestinationException("Kafka reads from topics, not from " + destination);
        }
        var unreachable = KafkaBootstrapProbe.unreachableMessage(endpoint.address(), connectTimeout);
        if (unreachable.isPresent()) {
            throw new ReadFailedException(unreachable.get());
        }
        Consumer<String, byte[]> consumer;
        try {
            consumer = createConsumer(endpoint);
        } catch (KafkaException e) {
            throw new ReadFailedException("Could not read from topic '" + topic + "': " + e.getMessage(), e);
        }
        try {
            var partitions = consumer.partitionsFor(topic, openTimeout).stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .toList();
            if (partitions.isEmpty()) {
                throw new ReadFailedException("Topic '" + topic + "' does not exist");
            }
            consumer.assign(partitions);
            seek(consumer, partitions, startPosition);
            return new KafkaMessageStream(consumer, topic);
        } catch (KafkaException e) {
            consumer.close();
            throw new ReadFailedException("Could not read from topic '" + topic + "': " + e.getMessage(), e);
        } catch (RuntimeException e) {
            consumer.close();
            throw e;
        }
    }

    private void seek(Consumer<String, byte[]> consumer, List<TopicPartition> partitions, StartPosition startPosition) {
        switch (startPosition) {
            case EARLIEST -> consumer.seekToBeginning(partitions);
            case LATEST -> consumer.seekToEnd(partitions);
        }
        // Seeking is lazy. Resolve the offsets now, or messages produced between opening and the
        // first poll would be skipped when starting from LATEST.
        partitions.forEach(partition -> consumer.position(partition, openTimeout));
    }

    private Consumer<String, byte[]> createConsumer(BrokerEndpoint endpoint) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                endpoint.address(),
                ConsumerConfig.CLIENT_ID_CONFIG,
                KafkaClientIds.next("reader"),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false,
                // Metadata requests from a consumer would otherwise create a missing topic.
                ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,
                false,
                ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,
                (int) openTimeout.toMillis());
        return new KafkaConsumer<>(config, new StringDeserializer(), new ByteArrayDeserializer());
    }
}
