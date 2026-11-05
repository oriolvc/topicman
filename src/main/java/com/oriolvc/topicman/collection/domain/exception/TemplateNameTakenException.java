package com.oriolvc.topicman.collection.domain.exception;

import com.oriolvc.topicman.collection.domain.model.TemplateName;
import com.oriolvc.topicman.shared.domain.DomainException;

/** Another template in the same collection already has this name. */
public final class TemplateNameTakenException extends DomainException {

    public static final String CODE = "collection.template-name-taken";

    public TemplateNameTakenException(TemplateName name) {
        super(CODE, "The collection already has a template named '" + name.value() + "'");
    }
}
