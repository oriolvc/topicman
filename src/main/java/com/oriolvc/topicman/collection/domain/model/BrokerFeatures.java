package com.oriolvc.topicman.collection.domain.model;

import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.port.SupportedBrokers;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/**
 * What a broker accepts from a template, as the messaging context reports it through
 * {@link SupportedBrokers} (ADR 0006).
 *
 * @param destinationKind the one kind of destination the broker accepts
 * @param supportsMessageKeys whether a message may carry a key
 */
public record BrokerFeatures(BrokerType brokerType, DestinationKind destinationKind, boolean supportsMessageKeys) {

    public BrokerFeatures {
        Objects.requireNonNull(brokerType, "brokerType");
        Objects.requireNonNull(destinationKind, "destinationKind");
    }

    /**
     * @throws InvalidTemplateException if the template's destination kind or key does not fit
     * @throws IllegalArgumentException if the template targets another broker
     */
    public void check(MessageTemplate template) {
        if (template.brokerType() != brokerType) {
            throw new IllegalArgumentException(
                    "Checked a " + template.brokerType() + " template against " + brokerType + " features");
        }
        if (template.destination().kind() != destinationKind) {
            throw new InvalidTemplateException(brokerType + " templates need a " + destinationKind
                    + " destination, not a " + template.destination().kind());
        }
        if (template.key() != null && !supportsMessageKeys) {
            throw new InvalidTemplateException(brokerType + " messages have no key; omit it");
        }
    }
}
