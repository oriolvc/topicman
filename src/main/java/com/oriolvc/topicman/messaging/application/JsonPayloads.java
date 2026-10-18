package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.PayloadFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Checks that JSON payloads are well-formed (the domain has no JSON parser). */
final class JsonPayloads {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private JsonPayloads() {}

    static void requireWellFormed(Payload payload) {
        if (payload.format() != PayloadFormat.JSON) {
            return;
        }
        try {
            JSON.readTree(payload.content());
        } catch (JacksonException e) {
            // Only the location: Jackson's message quotes the payload.
            var location = e.getLocation();
            var where =
                    location == null ? "" : " at line " + location.getLineNr() + ", column " + location.getColumnNr();
            throw new InvalidMessageException("Payload is not well-formed JSON" + where);
        }
    }
}
