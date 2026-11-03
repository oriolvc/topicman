package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** No collection has this id. */
public final class CollectionNotFoundException extends DomainException {

    public static final String CODE = "collection.not-found";

    public CollectionNotFoundException(CollectionId id) {
        super(CODE, "No collection has id " + id);
    }
}
