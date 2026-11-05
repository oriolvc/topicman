package com.oriolvc.topicman.collection.domain.exception;

import com.oriolvc.topicman.collection.domain.model.TemplateId;
import com.oriolvc.topicman.shared.domain.DomainException;

/** The collection has no template with this id. */
public final class TemplateNotFoundException extends DomainException {

    public static final String CODE = "collection.template-not-found";

    public TemplateNotFoundException(TemplateId id) {
        super(CODE, "No template has id " + id);
    }
}
