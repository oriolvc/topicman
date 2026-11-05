package com.oriolvc.topicman.collection.domain.model;

import com.oriolvc.topicman.collection.domain.exception.InvalidEnvironmentException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The name of an environment variable, as written in a {@code {{name}}} placeholder: a letter or
 * {@code _}, then letters, digits, {@code _}, {@code .} or {@code -}. Names starting with {@code $}
 * are reserved for built-in variables (ADR 0006).
 */
public record VariableName(String value) {

    static final String PATTERN = "[A-Za-z_][A-Za-z0-9_.-]*";

    private static final Pattern VALID = Pattern.compile(PATTERN);

    public VariableName {
        Objects.requireNonNull(value, "value");
        if (value.startsWith("$")) {
            throw new InvalidEnvironmentException(
                    "Variable '" + value + "' starts with $, which is reserved for built-in variables");
        }
        if (!VALID.matcher(value).matches()) {
            throw new InvalidEnvironmentException("Variable name '" + value
                    + "' must start with a letter or _ and contain only letters, digits, _, . and -");
        }
    }
}
