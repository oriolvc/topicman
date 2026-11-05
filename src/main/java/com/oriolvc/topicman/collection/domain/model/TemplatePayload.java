package com.oriolvc.topicman.collection.domain.model;

import java.util.Objects;

/** A template's message body, which may hold placeholders until the template is resolved. */
public record TemplatePayload(PayloadFormat format, String content) {

    public TemplatePayload {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(content, "content");
    }
}
