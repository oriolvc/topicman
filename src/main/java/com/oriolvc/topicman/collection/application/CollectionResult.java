package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.Collection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** @param templates in the collection's order */
public record CollectionResult(
        UUID id, String name, List<TemplateResult> templates, Instant createdAt, Instant updatedAt) {

    static CollectionResult of(Collection collection) {
        return new CollectionResult(
                collection.id().value(),
                collection.name().value(),
                collection.templates().stream().map(TemplateResult::of).toList(),
                collection.createdAt(),
                collection.updatedAt());
    }
}
