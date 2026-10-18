package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param sendTimeout how long a send waits for topic metadata, and then for the acknowledgement
 * @param openTimeout how long opening a reader waits for topic metadata and start offsets
 */
@ConfigurationProperties("topicman.kafka")
public record KafkaProperties(
        @DefaultValue("10s") Duration sendTimeout,
        @DefaultValue("10s") Duration openTimeout) {}
