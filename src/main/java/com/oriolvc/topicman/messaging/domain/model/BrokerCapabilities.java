package com.oriolvc.topicman.messaging.domain.model;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/**
 * What a broker can do, so clients can explain a limitation instead of hiding it (ADR 0003). Every
 * broker adapter must behave as its capabilities say; the shared adapter contract test checks it.
 *
 * @param destinationType the one kind of destination the broker accepts
 * @param canReadFromBeginning whether a consume session starting from {@link StartPosition#EARLIEST}
 *     sees retained messages; if not, it behaves like {@link StartPosition#LATEST}
 * @param supportsMessageKeys whether a produced message may carry a key
 */
public record BrokerCapabilities(
        BrokerType brokerType,
        DestinationType destinationType,
        boolean canReadFromBeginning,
        boolean supportsMessageKeys) {

    public BrokerCapabilities {
        Objects.requireNonNull(brokerType, "brokerType");
        Objects.requireNonNull(destinationType, "destinationType");
    }

    public static BrokerCapabilities of(BrokerType brokerType) {
        return switch (brokerType) {
            case KAFKA, PULSAR -> new BrokerCapabilities(brokerType, DestinationType.TOPIC, true, true);
            // A temporary queue only receives what is published after it is bound.
            case RABBITMQ -> new BrokerCapabilities(brokerType, DestinationType.EXCHANGE, false, false);
        };
    }

    public boolean supports(Destination destination) {
        return destination.type() == destinationType;
    }
}
