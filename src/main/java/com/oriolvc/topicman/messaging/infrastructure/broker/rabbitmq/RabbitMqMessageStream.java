package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.Headers;
import com.oriolvc.topicman.messaging.domain.model.Message;
import com.oriolvc.topicman.messaging.domain.model.Payload;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.port.MessageStream;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns the broker's pushed deliveries into a pull-based read. Deliveries wait in a local queue,
 * bounded by the channel's prefetch, and are acknowledged when polled, which lets the broker push
 * more. Acknowledging only affects the session's own temporary queue. Payloads are decoded as UTF-8
 * text, as for every broker.
 */
final class RabbitMqMessageStream implements MessageStream {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqMessageStream.class);

    private record Delivery(Envelope envelope, AMQP.BasicProperties properties, byte[] body, Instant receivedAt) {}

    private final Channel channel;
    private final String exchange;
    private final BlockingQueue<Delivery> deliveries = new LinkedBlockingQueue<>();
    private final Consumer consumer;
    private volatile @Nullable ShutdownSignalException shutdown;

    RabbitMqMessageStream(Channel channel, String exchange) {
        this.channel = channel;
        this.exchange = exchange;
        this.consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                deliveries.add(new Delivery(envelope, properties, body, Instant.now()));
            }

            @Override
            public void handleShutdownSignal(String consumerTag, ShutdownSignalException signal) {
                shutdown = signal;
            }
        };
    }

    Consumer consumer() {
        return consumer;
    }

    @Override
    public List<ReceivedMessage> poll(Duration timeout) {
        failIfShutDown();
        Delivery first;
        try {
            first = deliveries.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReadFailedException("Interrupted while reading from exchange '" + exchange + "'", e);
        }
        if (first == null) {
            failIfShutDown();
            return List.of();
        }
        var batch = new ArrayList<Delivery>();
        batch.add(first);
        deliveries.drainTo(batch);
        acknowledgeUpTo(batch.getLast());
        return batch.stream().map(RabbitMqMessageStream::toReceivedMessage).toList();
    }

    @Override
    public void close() {
        // Closing the channel cancels the consumer, and the broker then deletes the auto-delete queue.
        if (!channel.isOpen()) {
            return;
        }
        try {
            channel.close();
        } catch (Exception e) {
            log.debug("Could not close the RabbitMQ channel on exchange '{}' cleanly", exchange, e);
        }
    }

    private void failIfShutDown() {
        var signal = shutdown;
        if (signal != null && deliveries.isEmpty()) {
            throw new ReadFailedException(
                    "Lost the connection to exchange '" + exchange + "': " + AmqpErrors.describe(signal), signal);
        }
    }

    private void acknowledgeUpTo(Delivery last) {
        try {
            channel.basicAck(last.envelope().getDeliveryTag(), true);
        } catch (IOException | ShutdownSignalException e) {
            throw new ReadFailedException(
                    "Could not read from exchange '" + exchange + "': " + AmqpErrors.describe(e), e);
        }
    }

    private static ReceivedMessage toReceivedMessage(Delivery delivery) {
        var message =
                new Message(null, headersOf(delivery.properties()), Payload.text(new String(delivery.body(), UTF_8)));
        // AMQP timestamps are optional and have a resolution of one second.
        var timestamp = delivery.properties().getTimestamp() != null
                ? delivery.properties().getTimestamp().toInstant()
                : delivery.receivedAt();
        return new ReceivedMessage(
                message, timestamp, String.valueOf(delivery.envelope().getDeliveryTag()));
    }

    private static Headers headersOf(AMQP.BasicProperties properties) {
        var headers = new LinkedHashMap<String, String>();
        if (properties.getHeaders() != null) {
            properties.getHeaders().forEach((name, value) -> {
                if (name != null && !name.isBlank()) {
                    headers.put(name, textOf(value));
                }
            });
        }
        return Headers.of(headers);
    }

    // AMQP header values are typed: strings arrive as LongString, numbers and booleans as such.
    private static String textOf(@Nullable Object value) {
        return switch (value) {
            case null -> "";
            case byte[] bytes -> new String(bytes, UTF_8);
            default -> value.toString();
        };
    }
}
