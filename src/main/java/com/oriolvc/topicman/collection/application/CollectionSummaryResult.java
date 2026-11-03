package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.Collection;
import java.time.Instant;
import java.util.UUID;

public record CollectionSummaryResult(UUID id, String name, int templateCount, Instant createdAt, Instant updatedAt) {

    static CollectionSummaryResult of(Collection collection) {
        return new CollectionSummaryResult(
                collection.id().value(),
                collection.name().value(),
                collection.templates().size(),
                collection.createdAt(),
                collection.updatedAt());
    }
}
