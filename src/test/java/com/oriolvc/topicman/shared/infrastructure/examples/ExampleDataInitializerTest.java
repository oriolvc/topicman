package com.oriolvc.topicman.shared.infrastructure.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExampleDataInitializerTest {

    private static final String EMBEDDED = "jdbc:h2:file:/data/topicman";
    private static final String POSTGRESQL = "jdbc:postgresql://localhost/topicman";

    @Test
    void loadsOnTheEmbeddedDatabaseByDefault() {
        assertThat(ExampleDataInitializer.wanted(null, EMBEDDED)).isTrue();
    }

    @Test
    void neverLoadsOnPostgresqlByDefault() {
        assertThat(ExampleDataInitializer.wanted(null, POSTGRESQL)).isFalse();
    }

    @Test
    void loadsOnPostgresqlWhenAskedTo() {
        assertThat(ExampleDataInitializer.wanted(true, POSTGRESQL)).isTrue();
    }

    @Test
    void canBeSwitchedOffOnTheEmbeddedDatabase() {
        assertThat(ExampleDataInitializer.wanted(false, EMBEDDED)).isFalse();
    }
}
