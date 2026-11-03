package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import com.oriolvc.topicman.messaging.infrastructure.broker.ServerProbe;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Probes the bootstrap servers before a client is created: the Kafka client never gives up on a
 * refused connection, and reports a missing topic once its timeout runs out.
 */
final class KafkaBootstrapProbe {

    private KafkaBootstrapProbe() {}

    /**
     * @param bootstrapServers comma-separated {@code host:port} list
     * @return a message saying why no server could be reached, or empty if one accepted a connection
     */
    static Optional<String> unreachableMessage(String bootstrapServers, Duration connectTimeout) {
        var servers = List.of(bootstrapServers.split(",")).stream()
                .map(String::strip)
                .filter(server -> !server.isEmpty())
                .toList();
        return ServerProbe.unreachableReason(servers, connectTimeout)
                .map(reason -> "Cannot reach the Kafka broker at " + bootstrapServers + ": " + reason);
    }
}
