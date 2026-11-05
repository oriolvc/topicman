package com.oriolvc.topicman.shared.infrastructure;

import org.jspecify.annotations.Nullable;

/** Recognizes the default embedded database (ADR 0010). */
public final class EmbeddedDatabase {

    private static final String JDBC_URL_PREFIX = "jdbc:h2:";

    private EmbeddedDatabase() {}

    /**
     * Pass the URL of the connection in use, not the property: Docker Compose and Testcontainers
     * replace the property's value without changing it.
     */
    public static boolean isAt(@Nullable String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.startsWith(JDBC_URL_PREFIX);
    }
}
