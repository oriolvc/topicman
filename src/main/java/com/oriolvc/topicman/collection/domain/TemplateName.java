package com.oriolvc.topicman.collection.domain;

/** The name of a message template, unique within its collection. Surrounding whitespace is stripped. */
public record TemplateName(String value) {

    public TemplateName {
        value = Names.require(value, "Template", InvalidTemplateException::new);
    }
}
