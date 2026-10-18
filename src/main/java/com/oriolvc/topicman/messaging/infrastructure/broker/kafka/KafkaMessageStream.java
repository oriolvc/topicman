package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.record.RecordBatch;

/**
 * A pull-based read over assigned partitions. Records the domain would reject on produce (empty
 * keys, null header values, blank header names) are still valid in Kafka, so they are adapted
 * instead of failing the whole session. Payloads are decoded as UTF-8 text; recognizing JSON is
 * left to the application layer so every broker behaves the same.
 */
final class KafkaMessageStream implements MessageStream {

    private final Consumer<String, byte[]> consumer;
    private final String topic;

    KafkaMessageStream(Consumer<String, byte[]> consumer, String topic) {
        this.consumer = consumer;
        this.topic = topic;
    }

    @Override
    public List<ReceivedMessage> poll(Duration timeout) {
        try {
            var received = new ArrayList<ReceivedMessage>();
            consumer.poll(timeout).forEach(record -> received.add(toReceivedMessage(record)));
            return List.copyOf(received);
        } catch (KafkaException e) {
            throw new ReadFailedException("Could not read from topic '" + topic + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // Without a group there is nothing to commit or leave, so closing is immediate.
        consumer.close();
    }

    private static ReceivedMessage toReceivedMessage(ConsumerRecord<String, byte[]> record) {
        var key = record.key() == null || record.key().isEmpty() ? null : record.key();
        var value = record.value() == null ? "" : new String(record.value(), UTF_8);
        var message = new Message(key, headersOf(record), Payload.text(value));
        var position = record.topic() + "-" + record.partition() + "@" + record.offset();
        return new ReceivedMessage(message, timestampOf(record), position);
    }

    private static Headers headersOf(ConsumerRecord<?, ?> record) {
        var headers = new LinkedHashMap<String, String>();
        for (var header : record.headers()) {
            if (header.key() == null || header.key().isBlank()) {
                continue;
            }
            headers.put(header.key(), header.value() == null ? "" : new String(header.value(), UTF_8));
        }
        return Headers.of(headers);
    }

    private static Instant timestampOf(ConsumerRecord<?, ?> record) {
        // Records written with message format v0 carry no timestamp.
        return record.timestamp() == RecordBatch.NO_TIMESTAMP
                ? Instant.EPOCH
                : Instant.ofEpochMilli(record.timestamp());
    }
}
