package com.oriolvc.topicman.shared.domain;

/**
 * How a Kafka client sends a username and password (ADR 0008). Shared because the connection
 * context stores it and the messaging context's Kafka adapter uses it.
 */
public enum SaslMechanism {
    PLAIN,
    SCRAM_SHA_256,
    SCRAM_SHA_512
}
