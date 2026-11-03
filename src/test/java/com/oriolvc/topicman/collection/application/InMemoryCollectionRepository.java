package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.Collection;
import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
import com.oriolvc.topicman.collection.domain.TemplateId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores rehydrated copies, so a test only sees what a use case saved; ids come from counters. */
class InMemoryCollectionRepository implements CollectionRepository {

    private final Map<CollectionId, Collection> collections = new LinkedHashMap<>();
    private long nextId = 1;
    private long nextTemplateId = 1;

    @Override
    public CollectionId nextId() {
        return new CollectionId(nextId++);
    }

    @Override
    public TemplateId nextTemplateId() {
        return new TemplateId(nextTemplateId++);
    }

    @Override
    public void save(Collection collection) {
        var taken = collections.values().stream()
                .anyMatch(other ->
                        other.name().equals(collection.name()) && !other.id().equals(collection.id()));
        if (taken) {
            throw new CollectionNameTakenException(collection.name());
        }
        collections.put(collection.id(), copyOf(collection));
    }

    @Override
    public Optional<Collection> findById(CollectionId id) {
        return Optional.ofNullable(collections.get(id)).map(InMemoryCollectionRepository::copyOf);
    }

    @Override
    public List<Collection> findAll() {
        return collections.values().stream()
                .sorted(Comparator.comparing(collection -> collection.name().value()))
                .map(InMemoryCollectionRepository::copyOf)
                .toList();
    }

    @Override
    public boolean existsByName(CollectionName name) {
        return collections.values().stream()
                .anyMatch(collection -> collection.name().equals(name));
    }

    @Override
    public void deleteById(CollectionId id) {
        collections.remove(id);
    }

    private static Collection copyOf(Collection collection) {
        return Collection.rehydrate(
                collection.id(),
                collection.name(),
                collection.templates(),
                collection.createdAt(),
                collection.updatedAt());
    }
}
