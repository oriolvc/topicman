package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param sendTimeout how long a send waits for topic metadata, and then for the acknowledgement
 * @param openTimeout how long opening a reader waits for topic metadata and start offsets
 * @param connectTimeout how long producing or opening a reader first waits to connect to a bootstrap
 *     server, so an unreachable broker fails before the longer timeouts above
 */
@ConfigurationProperties("topicman.kafka")
public record KafkaProperties(
        @DefaultValue("10s") Duration sendTimeout,
        @DefaultValue("10s") Duration openTimeout,
        @DefaultValue("3s") Duration connectTimeout) {}
