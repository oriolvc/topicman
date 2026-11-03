package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** No environment has this id. */
public final class EnvironmentNotFoundException extends DomainException {

    public static final String CODE = "collection.environment-not-found";

    public EnvironmentNotFoundException(EnvironmentId id) {
        super(CODE, "No environment has id " + id);
    }
}
