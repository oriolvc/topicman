package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.Environment;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** @param variables names and values, in order */
public record EnvironmentResult(
        long id, String name, Map<String, String> variables, Instant createdAt, Instant updatedAt) {

    public static EnvironmentResult of(Environment environment) {
        var variables = new LinkedHashMap<String, String>();
        environment.variables().forEach((name, value) -> variables.put(name.value(), value));
        return new EnvironmentResult(
                environment.id().value(),
                environment.name().value(),
                Collections.unmodifiableMap(variables),
                environment.createdAt(),
                environment.updatedAt());
    }
}
