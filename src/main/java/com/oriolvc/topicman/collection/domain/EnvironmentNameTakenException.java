package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** Another environment already has this name. */
public final class EnvironmentNameTakenException extends DomainException {

    public static final String CODE = "collection.environment-name-taken";

    public EnvironmentNameTakenException(EnvironmentName name) {
        super(CODE, "An environment named '" + name.value() + "' already exists");
    }
}
