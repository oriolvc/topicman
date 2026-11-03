package com.oriolvc.topicman.collection.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One variable of an environment; the value may be empty and is inserted as written. */
public record Variable(VariableName name, String value) {

    public Variable {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
    }

    /**
     * The variables as an environment holds them, in order.
     *
     * @throws InvalidEnvironmentException if a name appears twice
     */
    public static Map<VariableName, String> toMap(List<Variable> variables) {
        var map = new LinkedHashMap<VariableName, String>();
        for (var variable : variables) {
            if (map.putIfAbsent(variable.name(), variable.value()) != null) {
                throw new InvalidEnvironmentException(
                        "Variable '" + variable.name().value() + "' is defined more than once");
            }
        }
        return map;
    }
}
