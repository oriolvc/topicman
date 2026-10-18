package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param sendTimeout how long a send waits for topic metadata, and then for the acknowledgement
 */
@ConfigurationProperties("topicman.kafka")
public record KafkaProperties(@DefaultValue("10s") Duration sendTimeout) {}
