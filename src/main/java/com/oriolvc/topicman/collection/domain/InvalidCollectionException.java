package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** A collection breaks a rule, such as a blank name. */
public final class InvalidCollectionException extends DomainException {

    public static final String CODE = "collection.invalid-collection";

    InvalidCollectionException(String message) {
        super(CODE, message);
    }
}
