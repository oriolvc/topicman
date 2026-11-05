package com.oriolvc.topicman.messaging.domain.model;

/** The kinds of {@link Destination}; each broker supports exactly one. */
public enum DestinationType {
    TOPIC,
    EXCHANGE
}
