package com.oriolvc.topicman.connection.domain;

import java.util.Objects;

/** The name a user gives a connection, stripped of surrounding whitespace. */
public record ConnectionName(String value) {

    public static final int MAX_LENGTH = 100;

    public ConnectionName {
        Objects.requireNonNull(value, "value");
        value = value.strip();
        if (value.isEmpty()) {
            throw new InvalidConnectionException("Connection name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidConnectionException("Connection name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
