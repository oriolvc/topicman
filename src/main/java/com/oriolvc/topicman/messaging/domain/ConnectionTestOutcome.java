package com.oriolvc.topicman.messaging.domain;

import java.util.Objects;

/**
 * Whether a broker answered a connection test as expected.
 *
 * @param detail for people: what answered on success, why it failed otherwise; never credentials
 */
public record ConnectionTestOutcome(boolean succeeded, String detail) {

    public ConnectionTestOutcome {
        Objects.requireNonNull(detail, "detail");
        if (detail.isBlank()) {
            throw new IllegalArgumentException("A connection test outcome needs a detail");
        }
    }

    public static ConnectionTestOutcome succeeded(String detail) {
        return new ConnectionTestOutcome(true, detail);
    }

    public static ConnectionTestOutcome failed(String detail) {
        return new ConnectionTestOutcome(false, detail);
    }
}
