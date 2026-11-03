package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;
import java.util.ArrayList;
import java.util.List;

/**
 * A template could not be resolved: some placeholders have no value, or name a built-in variable
 * that does not exist. Every problem is listed at once, so they can all be fixed together.
 */
public final class UnresolvedVariablesException extends DomainException {

    public static final String CODE = "collection.unresolved-variables";

    private final List<String> missing;
    private final List<String> unknownBuiltIns;

    UnresolvedVariablesException(List<String> missing, List<String> unknownBuiltIns) {
        super(CODE, message(missing, unknownBuiltIns));
        this.missing = List.copyOf(missing);
        this.unknownBuiltIns = List.copyOf(unknownBuiltIns);
    }

    /** Variables without a value, in the order they first appear. */
    public List<String> missing() {
        return missing;
    }

    /** {@code $} names that are not built-in variables, in the order they first appear. */
    public List<String> unknownBuiltIns() {
        return unknownBuiltIns;
    }

    private static String message(List<String> missing, List<String> unknownBuiltIns) {
        var parts = new ArrayList<String>();
        if (!missing.isEmpty()) {
            parts.add("No value for " + placeholders(missing));
        }
        if (!unknownBuiltIns.isEmpty()) {
            parts.add("No built-in variable " + placeholders(unknownBuiltIns));
        }
        return String.join("; ", parts);
    }

    private static String placeholders(List<String> names) {
        return String.join(", ", names.stream().map(name -> "{{" + name + "}}").toList());
    }
}
