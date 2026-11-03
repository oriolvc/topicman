package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import com.oriolvc.topicman.messaging.infrastructure.broker.ServerProbe;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Probes the hosts of a service URL before a producer or reader is created: the Pulsar client
 * retries a refused connection until its operation timeout runs out.
 */
final class PulsarServiceUrlProbe {

    private PulsarServiceUrlProbe() {}

    /**
     * @param serviceUrl a URL the Pulsar client already accepted, such as {@code
     *     pulsar://host1:6650,host2:6650} or {@code http://host:8080}
     * @return a message saying why no host could be reached, or empty if one accepted a connection
     */
    static Optional<String> unreachableMessage(String serviceUrl, Duration connectTimeout) {
        return ServerProbe.unreachableReason(servers(serviceUrl), connectTimeout)
                .map(reason -> "Cannot reach the Pulsar broker at " + serviceUrl + ": " + reason);
    }

    /** The {@code host:port} entries of a service URL, with the scheme's default port where none is given. */
    static List<String> servers(String serviceUrl) {
        var schemeEnd = serviceUrl.indexOf("://");
        var scheme = schemeEnd < 0 ? "" : serviceUrl.substring(0, schemeEnd);
        var hosts = schemeEnd < 0 ? serviceUrl : serviceUrl.substring(schemeEnd + 3);
        var pathStart = hosts.indexOf('/');
        if (pathStart >= 0) {
            hosts = hosts.substring(0, pathStart);
        }
        var defaultPort = defaultPort(scheme);
        return List.of(hosts.split(",")).stream()
                .map(String::strip)
                .filter(host -> !host.isEmpty())
                .map(host -> hasPort(host) ? host : host + ":" + defaultPort)
                .toList();
    }

    private static boolean hasPort(String host) {
        var lastColon = host.lastIndexOf(':');
        return lastColon > host.lastIndexOf(']');
    }

    private static int defaultPort(String scheme) {
        return switch (scheme) {
            case "pulsar+ssl" -> 6651;
            case "http" -> 80;
            case "https" -> 443;
            default -> 6650;
        };
    }
}
