package com.oriolvc.topicman.collection.domain.model;

import com.oriolvc.topicman.collection.domain.exception.InvalidCollectionException;

/** The name of a collection, unique among collections. Surrounding whitespace is stripped. */
public record CollectionName(String value) {

    public CollectionName {
        value = Names.require(value, "Collection", InvalidCollectionException::new);
    }
}
