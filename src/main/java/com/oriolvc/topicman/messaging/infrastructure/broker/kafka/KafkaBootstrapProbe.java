package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks that at least one bootstrap server accepts a TCP connection. The Kafka client never gives
 * up on a refused connection: it retries until its timeout and then reports a missing topic, which
 * hides the real problem. Opening a socket first turns that into an immediate, clear failure.
 */
final class KafkaBootstrapProbe {

    private KafkaBootstrapProbe() {}

    /**
     * @param bootstrapServers comma-separated {@code host:port} list, IPv6 hosts in brackets
     * @return why none of the servers could be reached, or empty if one of them accepted a connection
     */
    static Optional<String> unreachableReason(String bootstrapServers, Duration connectTimeout) {
        var reasons = new ArrayList<String>();
        for (var server : servers(bootstrapServers)) {
            try (var socket = new Socket()) {
                socket.connect(toAddress(server), (int) connectTimeout.toMillis());
                return Optional.empty();
            } catch (IOException | IllegalArgumentException e) {
                reasons.add(server + " (" + describe(e) + ")");
            }
        }
        return Optional.of(String.join(", ", reasons));
    }

    private static List<String> servers(String bootstrapServers) {
        return List.of(bootstrapServers.split(",")).stream()
                .map(String::strip)
                .filter(server -> !server.isEmpty())
                .toList();
    }

    private static InetSocketAddress toAddress(String server) {
        var separator = server.lastIndexOf(':');
        if (separator <= 0 || separator == server.length() - 1) {
            throw new IllegalArgumentException("expected host:port");
        }
        var host = server.substring(0, separator);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        int port;
        try {
            port = Integer.parseInt(server.substring(separator + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid port", e);
        }
        return new InetSocketAddress(host, port);
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
