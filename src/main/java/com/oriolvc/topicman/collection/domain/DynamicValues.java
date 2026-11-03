package com.oriolvc.topicman.collection.domain;

import java.time.Instant;
import java.util.UUID;

/** Outbound port: the source of the built-in variables' values, so resolution can be tested. */
public interface DynamicValues {

    /** For {@code {{$uuid}}}. */
    UUID randomUuid();

    /** For {@code {{$timestamp}}} and {@code {{$isoTimestamp}}}. */
    Instant now();

    /** For {@code {{$randomInt}}}: between 0 and 1000, both included. */
    int randomInt();
}
