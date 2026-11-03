package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pull-based read over a Pulsar reader. Messages the domain would reject on produce (empty keys,
 * blank property names) are still valid in Pulsar, so they are adapted instead of failing the
 * session. Payloads are decoded as UTF-8 text, as for every broker.
 */
final class PulsarMessageStream implements MessageStream {

    private static final Logger log = LoggerFactory.getLogger(PulsarMessageStream.class);

    // Bounds one poll, so a large backlog is handed over in chunks rather than all at once.
    private static final int MAX_MESSAGES_PER_POLL = 500;

    private final Reader<byte[]> reader;
    private final String topic;

    PulsarMessageStream(Reader<byte[]> reader, String topic) {
        this.reader = reader;
        this.topic = topic;
    }

    @Override
    public List<ReceivedMessage> poll(Duration timeout) {
        try {
            var first = reader.readNext((int) timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (first == null) {
                return List.of();
            }
            var received = new ArrayList<ReceivedMessage>();
            received.add(toReceivedMessage(first));
            // A zero timeout only takes what the client already prefetched, without waiting.
            while (received.size() < MAX_MESSAGES_PER_POLL) {
                var next = reader.readNext(0, TimeUnit.MILLISECONDS);
                if (next == null) {
                    break;
                }
                received.add(toReceivedMessage(next));
            }
            return List.copyOf(received);
        } catch (PulsarClientException e) {
            throw new ReadFailedException("Could not read from topic '" + topic + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // Closing the reader removes its non-durable subscription from the broker.
        try {
            reader.close();
        } catch (IOException e) {
            log.debug("Could not close the Pulsar reader on topic '{}' cleanly", topic, e);
        }
    }

    private static ReceivedMessage toReceivedMessage(org.apache.pulsar.client.api.Message<byte[]> received) {
        var key = received.hasKey() && !received.getKey().isEmpty() ? received.getKey() : null;
        var message = new Message(key, headersOf(received), Payload.text(new String(received.getData(), UTF_8)));
        return new ReceivedMessage(
                message,
                Instant.ofEpochMilli(received.getPublishTime()),
                received.getMessageId().toString());
    }

    private static Headers headersOf(org.apache.pulsar.client.api.Message<byte[]> received) {
        var headers = new LinkedHashMap<String, String>();
        received.getProperties().forEach((name, value) -> {
            if (name != null && !name.isBlank()) {
                headers.put(name, value == null ? "" : value);
            }
        });
        return Headers.of(headers);
    }
}
