package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import com.oriolvc.topicman.messaging.domain.exception.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.messaging.domain.port.MessageReader;
import com.oriolvc.topicman.messaging.domain.port.MessageStream;
import com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq.RabbitMqConnections.ConnectionFailedException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads an exchange without affecting real consumers (ADR 0003): each session declares its own
 * server-named, exclusive, auto-delete queue and binds it to the exchange, so it gets copies of
 * the messages routed there and never takes messages from an existing queue. The queue is deleted
 * when the session closes. Such a queue only sees messages published after it was bound, so
 * {@link StartPosition#EARLIEST} behaves like {@link StartPosition#LATEST}.
 */
final class RabbitMqMessageReader implements MessageReader {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqMessageReader.class);

    // Unacknowledged deliveries the broker may push before the session polls; bounds memory use.
    private static final int PREFETCH = 500;

    private final RabbitMqConnections connections;

    RabbitMqMessageReader(RabbitMqConnections connections) {
        this.connections = connections;
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.RABBITMQ;
    }

    @Override
    public MessageStream open(BrokerEndpoint endpoint, Destination destination, StartPosition startPosition) {
        if (!(destination instanceof Destination.Exchange(var exchange, var routingKey))) {
            throw new InvalidDestinationException("RabbitMQ reads from exchanges, not from " + destination);
        }
        com.rabbitmq.client.Connection connection;
        try {
            connection = connections.forEndpoint(endpoint);
        } catch (ConnectionFailedException e) {
            throw new ReadFailedException(e.getMessage());
        }
        Channel channel = null;
        try {
            channel = connection.createChannel();
            // Checked before declaring the queue: a failed bind would leave the queue behind, as an
            // exclusive queue lives as long as the shared connection unless a consumer used it.
            channel.exchangeDeclarePassive(exchange);
            var queue = channel.queueDeclare("", false, true, true, null).getQueue();
            channel.queueBind(queue, exchange, routingKey);
            channel.basicQos(PREFETCH);
            var stream = new RabbitMqMessageStream(channel, exchange);
            channel.basicConsume(queue, false, stream.consumer());
            return stream;
        } catch (IOException | ShutdownSignalException e) {
            closeQuietly(channel);
            if (AmqpErrors.isNotFound(e)) {
                throw new ReadFailedException("Exchange '" + exchange + "' does not exist", e);
            }
            throw new ReadFailedException(
                    "Could not read from exchange '" + exchange + "': " + AmqpErrors.describe(e), e);
        }
    }

    private static void closeQuietly(@Nullable Channel channel) {
        if (channel == null || !channel.isOpen()) {
            return;
        }
        try {
            channel.close();
        } catch (Exception e) {
            log.debug("Could not close a RabbitMQ channel cleanly", e);
        }
    }
}
