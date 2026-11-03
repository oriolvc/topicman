package com.oriolvc.topicman.collection.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Replaces the {@code {{variable}}} placeholders of a template with an environment's values and
 * the built-in variables' generated values (ADR 0006). Resolution is a single pass, so inserted
 * values are never resolved again, and it fails as a whole, listing every missing variable, rather
 * than leave a placeholder in a message.
 */
public final class TemplateResolver {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*(\\$?" + VariableName.PATTERN + ")\\s*\\}\\}");

    private final DynamicValues dynamicValues;

    public TemplateResolver(DynamicValues dynamicValues) {
        this.dynamicValues = Objects.requireNonNull(dynamicValues, "dynamicValues");
    }

    /**
     * @param environment the selected environment, or {@code null} when none is: only built-in
     *     variables resolve then
     * @throws UnresolvedVariablesException if a placeholder has no value
     * @throws InvalidTemplateException if the destination or a header name resolves to nothing
     */
    public ResolvedMessage resolve(MessageTemplate template, @Nullable Environment environment) {
        var values = valuesFor(placeholdersIn(template), environment);
        return new ResolvedMessage(
                template.brokerType(),
                resolve(template.destination(), values),
                resolveKey(template.key(), values),
                resolveHeaders(template.headers(), values),
                new TemplatePayload(
                        template.payload().format(), replace(template.payload().content(), values)));
    }

    /** Every placeholder name, in the order it first appears in the message. */
    private static LinkedHashSet<String> placeholdersIn(MessageTemplate template) {
        var texts = new ArrayList<String>();
        switch (template.destination()) {
            case TemplateDestination.Topic(var name) -> texts.add(name);
            case TemplateDestination.Exchange(var name, var routingKey) -> {
                texts.add(name);
                texts.add(routingKey);
            }
        }
        if (template.key() != null) {
            texts.add(template.key());
        }
        template.headers().forEach((name, value) -> {
            texts.add(name);
            texts.add(value);
        });
        texts.add(template.payload().content());

        var names = new LinkedHashSet<String>();
        for (var text : texts) {
            var matcher = PLACEHOLDER.matcher(text);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    private Map<String, String> valuesFor(LinkedHashSet<String> names, @Nullable Environment environment) {
        var values = new HashMap<String, String>();
        var missing = new ArrayList<String>();
        var unknownBuiltIns = new ArrayList<String>();
        var builtIns = new BuiltIns(dynamicValues);
        for (var name : names) {
            if (name.startsWith("$")) {
                var value = builtIns.valueOf(name);
                if (value == null) {
                    unknownBuiltIns.add(name);
                } else {
                    values.put(name, value);
                }
            } else {
                var value =
                        environment == null ? null : environment.valueOf(name).orElse(null);
                if (value == null) {
                    missing.add(name);
                } else {
                    values.put(name, value);
                }
            }
        }
        if (!missing.isEmpty() || !unknownBuiltIns.isEmpty()) {
            throw new UnresolvedVariablesException(missing, unknownBuiltIns);
        }
        return values;
    }

    private static TemplateDestination resolve(TemplateDestination destination, Map<String, String> values) {
        return switch (destination) {
            case TemplateDestination.Topic(var name) -> TemplateDestination.topic(replace(name, values));
            case TemplateDestination.Exchange(var name, var routingKey) ->
                TemplateDestination.exchange(replace(name, values), replace(routingKey, values));
        };
    }

    /** A key that resolves to nothing is dropped, as producing an empty key is not allowed. */
    private static @Nullable String resolveKey(@Nullable String key, Map<String, String> values) {
        if (key == null) {
            return null;
        }
        var resolved = replace(key, values);
        return resolved.isEmpty() ? null : resolved;
    }

    private static Map<String, String> resolveHeaders(Map<String, String> headers, Map<String, String> values) {
        var resolved = new LinkedHashMap<String, String>();
        headers.forEach((name, value) -> {
            var resolvedName = replace(name, values);
            if (resolvedName.isBlank()) {
                throw new InvalidTemplateException("Header name '" + name + "' resolves to nothing");
            }
            resolved.put(resolvedName, replace(value, values));
        });
        return resolved;
    }

    private static String replace(String text, Map<String, String> values) {
        // quoteReplacement keeps $ and \ in values literal.
        return PLACEHOLDER.matcher(text).replaceAll(match -> Matcher.quoteReplacement(values.get(match.group(1))));
    }

    /** The built-in variables of one resolution, each generated at most once. */
    private static final class BuiltIns {

        private final DynamicValues source;
        private @Nullable Instant now;

        BuiltIns(DynamicValues source) {
            this.source = source;
        }

        /** The value of a built-in variable, or {@code null} if there is none with this name. */
        @Nullable String valueOf(String name) {
            return switch (name) {
                case "$uuid" -> source.randomUuid().toString();
                case "$timestamp" -> String.valueOf(now().toEpochMilli());
                case "$isoTimestamp" -> now().truncatedTo(ChronoUnit.MILLIS).toString();
                case "$randomInt" -> String.valueOf(source.randomInt());
                default -> null;
            };
        }

        // $timestamp and $isoTimestamp in one message describe the same instant.
        private Instant now() {
            if (now == null) {
                now = source.now();
            }
            return now;
        }
    }
}
