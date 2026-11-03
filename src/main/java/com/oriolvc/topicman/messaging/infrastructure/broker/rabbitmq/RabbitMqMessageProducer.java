package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.PayloadFormat;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq.RabbitMqConnections.ConnectionFailedException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Publishes to an exchange with publisher confirms, so a send succeeds only once the broker took
 * responsibility for the message. Each send uses its own channel: a channel is not thread-safe,
 * and the broker closes it when the exchange does not exist, which must not affect other sends.
 */
final class RabbitMqMessageProducer implements MessageProducer {

    // Persistent, so a message routed to a durable queue survives a broker restart.
    private static final int PERSISTENT = 2;

    private final RabbitMqConnections connections;
    private final Duration sendTimeout;

    RabbitMqMessageProducer(RabbitMqConnections connections, RabbitMqProperties properties) {
        this.connections = connections;
        this.sendTimeout = properties.sendTimeout();
    }

    @Override
    public BrokerType brokerType() {
        return BrokerType.RABBITMQ;
    }

    @Override
    public SendReceipt send(BrokerEndpoint endpoint, Destination destination, Message message) {
        if (!(destination instanceof Destination.Exchange(var exchange, var routingKey))) {
            throw new InvalidDestinationException("RabbitMQ produces to exchanges, not to " + destination);
        }
        if (message.key() != null) {
            throw new InvalidMessageException(
                    "RabbitMQ messages have no key; they are routed by the destination's routing key");
        }
        com.rabbitmq.client.Connection connection;
        try {
            connection = connections.forEndpoint(endpoint);
        } catch (ConnectionFailedException e) {
            throw new ProduceFailedException(e.getMessage());
        }
        try (var channel = connection.createChannel()) {
            channel.confirmSelect();
            channel.basicPublish(
                    exchange,
                    routingKey,
                    propertiesOf(message),
                    message.payload().bytes());
            channel.waitForConfirmsOrDie(sendTimeout.toMillis());
            return new SendReceipt("confirmed by exchange '" + exchange + "'");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProduceFailedException("Interrupted while producing to exchange '" + exchange + "'", e);
        } catch (TimeoutException e) {
            throw new ProduceFailedException(
                    "Exchange '" + exchange + "' did not confirm the message within " + sendTimeout.toSeconds() + "s",
                    e);
        } catch (IOException | ShutdownSignalException e) {
            if (AmqpErrors.isNotFound(e)) {
                throw new ProduceFailedException("Exchange '" + exchange + "' does not exist", e);
            }
            throw new ProduceFailedException(
                    "Could not produce to exchange '" + exchange + "': " + AmqpErrors.describe(e), e);
        }
    }

    private static AMQP.BasicProperties propertiesOf(Message message) {
        Map<String, Object> headers = new LinkedHashMap<>(message.headers().values());
        return new AMQP.BasicProperties.Builder()
                .contentType(message.payload().format() == PayloadFormat.JSON ? "application/json" : "text/plain")
                .headers(headers)
                .timestamp(new Date())
                .deliveryMode(PERSISTENT)
                .build();
    }
}
