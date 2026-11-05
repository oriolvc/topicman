package com.oriolvc.topicman.collection.domain.exception;

import com.oriolvc.topicman.shared.domain.DomainException;

/** An environment breaks a rule, such as a variable name that cannot appear in a placeholder. */
public final class InvalidEnvironmentException extends DomainException {

    public static final String CODE = "collection.invalid-environment";

    public InvalidEnvironmentException(String message) {
        super(CODE, message);
    }
}
