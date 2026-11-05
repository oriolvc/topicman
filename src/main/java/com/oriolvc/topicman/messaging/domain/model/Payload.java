package com.oriolvc.topicman.messaging.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The message body. Brokers transport bytes; TopicMan always encodes the content as UTF-8.
 *
 * <p>JSON well-formedness is checked in the application layer before producing, because it needs a
 * JSON parser and the domain has no framework dependencies.
 */
public record Payload(PayloadFormat format, String content) {

    public Payload {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(content, "content");
    }

    public static Payload json(String content) {
        return new Payload(PayloadFormat.JSON, content);
    }

    public static Payload text(String content) {
        return new Payload(PayloadFormat.TEXT, content);
    }

    public byte[] bytes() {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
