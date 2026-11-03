package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param sendTimeout how long creating a producer, and then a send, waits for the broker
 * @param openTimeout how long opening a reader waits for the topic lookup and the reader
 * @param connectTimeout how long producing or opening a reader first waits to connect to a broker,
 *     so an unreachable broker fails before the longer timeouts above; the client uses it too
 */
@ConfigurationProperties("topicman.pulsar")
public record PulsarProperties(
        @DefaultValue("10s") Duration sendTimeout,
        @DefaultValue("10s") Duration openTimeout,
        @DefaultValue("3s") Duration connectTimeout) {}
