package com.oriolvc.topicman.collection.domain.model;

import com.oriolvc.topicman.collection.domain.exception.InvalidEnvironmentException;
import com.oriolvc.topicman.collection.domain.port.EnvironmentRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A named set of variable values, such as {@code dev} or {@code staging}. Environments are global:
 * one applies to every collection (ADR 0006). Names are unique, which the repository enforces.
 */
public final class Environment {

    private final EnvironmentId id;
    private final Instant createdAt;
    private EnvironmentName name;
    private Map<VariableName, String> variables;
    private Instant updatedAt;

    private Environment(
            EnvironmentId id,
            EnvironmentName name,
            Map<VariableName, String> variables,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.variables = copyOf(variables);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** @param id a new id, from {@link EnvironmentRepository#nextId()} */
    public static Environment create(
            EnvironmentId id, EnvironmentName name, Map<VariableName, String> variables, Instant now) {
        return new Environment(id, name, variables, now, now);
    }

    public static Environment rehydrate(
            EnvironmentId id,
            EnvironmentName name,
            Map<VariableName, String> variables,
            Instant createdAt,
            Instant updatedAt) {
        return new Environment(id, name, variables, createdAt, updatedAt);
    }

    public void rename(EnvironmentName newName, Instant now) {
        name = Objects.requireNonNull(newName, "newName");
        touch(now);
    }

    public void replaceVariables(Map<VariableName, String> newVariables, Instant now) {
        variables = copyOf(newVariables);
        touch(now);
    }

    /** The value of a variable, which may be empty. */
    public Optional<String> valueOf(String variableName) {
        return variables.entrySet().stream()
                .filter(entry -> entry.getKey().value().equals(variableName))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static Map<VariableName, String> copyOf(Map<VariableName, String> variables) {
        Objects.requireNonNull(variables, "variables");
        var copy = new LinkedHashMap<VariableName, String>();
        variables.forEach((name, value) -> {
            Objects.requireNonNull(name, "variable name");
            if (value == null) {
                throw new InvalidEnvironmentException("Variable '" + name.value() + "' must have a value");
            }
            copy.put(name, value);
        });
        return Collections.unmodifiableMap(copy);
    }

    private void touch(Instant now) {
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public EnvironmentId id() {
        return id;
    }

    public EnvironmentName name() {
        return name;
    }

    /** The variables, in order. */
    public Map<VariableName, String> variables() {
        return variables;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Environment environment && id.equals(environment.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
