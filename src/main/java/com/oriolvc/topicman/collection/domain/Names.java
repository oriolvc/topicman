package com.oriolvc.topicman.collection.domain;

import java.util.Objects;
import java.util.function.Function;

/** The rule shared by the names of collections, templates and environments. */
final class Names {

    static final int MAX_LENGTH = 100;

    private Names() {}

    /** The name stripped of surrounding whitespace, if it is not blank and not too long. */
    static String require(String value, String what, Function<String, RuntimeException> invalid) {
        Objects.requireNonNull(value, "value");
        var stripped = value.strip();
        if (stripped.isEmpty()) {
            throw invalid.apply(what + " name must not be blank");
        }
        if (stripped.length() > MAX_LENGTH) {
            throw invalid.apply(what + " name must be at most " + MAX_LENGTH + " characters");
        }
        return stripped;
    }
}
