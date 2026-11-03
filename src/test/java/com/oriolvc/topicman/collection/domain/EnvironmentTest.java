package com.oriolvc.topicman.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvironmentTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-03T10:00:00Z");
    private static final Instant LATER = CREATED_AT.plusSeconds(60);

    @Test
    void keepsVariablesInOrder() {
        var variables = new LinkedHashMap<VariableName, String>();
        variables.put(new VariableName("env"), "stg");
        variables.put(new VariableName("orderId"), "42");

        var environment =
                Environment.create(TestIds.environmentId(), new EnvironmentName("staging"), variables, CREATED_AT);

        assertThat(environment.variables())
                .containsExactly(
                        Map.entry(new VariableName("env"), "stg"), Map.entry(new VariableName("orderId"), "42"));
        assertThat(environment.valueOf("orderId")).contains("42");
        assertThat(environment.valueOf("missing")).isEmpty();
    }

    @Test
    void allowsAnEmptyValue() {
        var environment = Environment.create(
                TestIds.environmentId(),
                new EnvironmentName("local"),
                Map.of(new VariableName("suffix"), ""),
                CREATED_AT);

        assertThat(environment.valueOf("suffix")).contains("");
    }

    @Test
    void replacesItsVariables() {
        var environment = Environment.create(
                TestIds.environmentId(),
                new EnvironmentName("staging"),
                Map.of(new VariableName("env"), "stg"),
                CREATED_AT);

        environment.replaceVariables(Map.of(new VariableName("region"), "eu"), LATER);

        assertThat(environment.variables()).containsOnlyKeys(new VariableName("region"));
        assertThat(environment.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void renames() {
        var environment =
                Environment.create(TestIds.environmentId(), new EnvironmentName("staging"), Map.of(), CREATED_AT);

        environment.rename(new EnvironmentName("stage"), LATER);

        assertThat(environment.name()).isEqualTo(new EnvironmentName("stage"));
        assertThat(environment.updatedAt()).isEqualTo(LATER);
    }
}
