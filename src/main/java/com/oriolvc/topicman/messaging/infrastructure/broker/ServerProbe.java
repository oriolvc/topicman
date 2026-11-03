package com.oriolvc.topicman.messaging.infrastructure.broker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks that at least one broker server accepts a TCP connection. Broker clients retry a refused
 * connection until their operation timeout and then report something unrelated, such as a missing
 * topic. Adapters open a socket first, which turns that into an immediate, clear failure.
 */
public final class ServerProbe {

    private ServerProbe() {}

    /**
     * @param servers {@code host:port} entries, IPv6 hosts in brackets
     * @return why none of the servers could be reached, or empty if one of them accepted a connection
     */
    public static Optional<String> unreachableReason(List<String> servers, Duration connectTimeout) {
        var reasons = new ArrayList<String>();
        for (var server : servers) {
            try (var socket = new Socket()) {
                socket.connect(toAddress(server), (int) connectTimeout.toMillis());
                return Optional.empty();
            } catch (IOException | IllegalArgumentException e) {
                reasons.add(server + " (" + describe(e) + ")");
            }
        }
        return Optional.of(reasons.isEmpty() ? "no server address given" : String.join(", ", reasons));
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
