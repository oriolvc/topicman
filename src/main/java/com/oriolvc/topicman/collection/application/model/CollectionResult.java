package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.Collection;
import java.time.Instant;
import java.util.List;

/** @param templates in the collection's order */
public record CollectionResult(
        long id, String name, List<TemplateResult> templates, Instant createdAt, Instant updatedAt) {

    public static CollectionResult of(Collection collection) {
        return new CollectionResult(
                collection.id().value(),
                collection.name().value(),
                collection.templates().stream().map(TemplateResult::of).toList(),
                collection.createdAt(),
                collection.updatedAt());
    }
}
