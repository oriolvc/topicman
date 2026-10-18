package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Unique client ids with a recognizable prefix, so broker operators can tell TopicMan's clients
 * apart. Kafka registers a JMX bean per client id and warns when two clients share one.
 */
final class KafkaClientIds {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private KafkaClientIds() {}

    static String next(String role) {
        return "topicman-" + role + "-" + SEQUENCE.incrementAndGet();
    }
}
