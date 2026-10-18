package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.PayloadFormat;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Recognizes JSON payloads (the domain has no JSON parser). */
final class JsonPayloads {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private JsonPayloads() {}

    /** Consumed payloads arrive as text; those that parse as JSON are marked as JSON. */
    static ReceivedMessage markJson(ReceivedMessage received) {
        var payload = received.message().payload();
        if (payload.format() == PayloadFormat.JSON || !isWellFormed(payload.content())) {
            return received;
        }
        var message =
                new Message(received.message().key(), received.message().headers(), Payload.json(payload.content()));
        return new ReceivedMessage(message, received.timestamp(), received.position());
    }

    private static boolean isWellFormed(String content) {
        try {
            JSON.readTree(content);
            return !content.isBlank();
        } catch (JacksonException e) {
            return false;
        }
    }

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
