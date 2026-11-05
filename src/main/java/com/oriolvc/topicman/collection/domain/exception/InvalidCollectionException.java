package com.oriolvc.topicman.collection.domain.exception;

import com.oriolvc.topicman.shared.domain.DomainException;

/** A collection breaks a rule, such as a blank name. */
public final class InvalidCollectionException extends DomainException {

    public static final String CODE = "collection.invalid-collection";

    public InvalidCollectionException(String message) {
        super(CODE, message);
    }
}
