package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param sendTimeout how long a publish waits for the broker's confirm
 * @param openTimeout how long the AMQP handshake and each channel operation (declaring the
 *     temporary queue, binding it) wait for the broker
 * @param connectTimeout how long opening the TCP connection waits
 */
@ConfigurationProperties("topicman.rabbitmq")
public record RabbitMqProperties(
        @DefaultValue("10s") Duration sendTimeout,
        @DefaultValue("10s") Duration openTimeout,
        @DefaultValue("3s") Duration connectTimeout) {}
