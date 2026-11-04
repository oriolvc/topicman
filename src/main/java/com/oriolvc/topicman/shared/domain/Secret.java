package com.oriolvc.topicman.shared.domain;

import java.util.Objects;

/**
 * A credential value such as a password or token. Its {@code toString()} is masked so a secret
 * cannot leak through logs, exception messages or test output (ADR 0005). Shared by the connection
 * context, which stores secrets, and the messaging context, which hands them to the broker clients
 * (ADR 0008). Whitespace is kept: it may be part of the secret.
 */
public record Secret(String value) {

    public Secret {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Secret must not be empty");
        }
    }

    @Override
    public String toString() {
        return "****";
    }
}
