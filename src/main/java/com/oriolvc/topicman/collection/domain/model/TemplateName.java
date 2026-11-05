package com.oriolvc.topicman.collection.domain.model;

import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;

/** The name of a message template, unique within its collection. Surrounding whitespace is stripped. */
public record TemplateName(String value) {

    public TemplateName {
        value = Names.require(value, "Template", InvalidTemplateException::new);
    }
}
