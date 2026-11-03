package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerCapabilities;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/**
 * A broker this server can produce to and read from, and what it can do. Use cases return it
 * instead of the domain's {@link BrokerCapabilities}, so other contexts can rely on it without
 * depending on the messaging domain.
 *
 * @param destinationKind the one kind of destination the broker accepts
 * @param canReadFromBeginning whether a consume session can start from the earliest message
 * @param supportsMessageKeys whether a produced message may carry a key
 */
public record SupportedBroker(
        BrokerType type, DestinationKind destinationKind, boolean canReadFromBeginning, boolean supportsMessageKeys) {

    public enum DestinationKind {
        TOPIC,
        EXCHANGE
    }

    public SupportedBroker {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(destinationKind, "destinationKind");
    }

    static SupportedBroker of(BrokerCapabilities capabilities) {
        return new SupportedBroker(
                capabilities.brokerType(),
                DestinationKind.valueOf(capabilities.destinationType().name()),
                capabilities.canReadFromBeginning(),
                capabilities.supportsMessageKeys());
    }
}
