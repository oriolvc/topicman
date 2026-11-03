package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** Another collection already has this name. */
public final class CollectionNameTakenException extends DomainException {

    public static final String CODE = "collection.name-taken";

    public CollectionNameTakenException(CollectionName name) {
        super(CODE, "A collection named '" + name.value() + "' already exists");
    }
}
