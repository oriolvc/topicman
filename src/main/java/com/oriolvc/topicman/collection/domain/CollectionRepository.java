package com.oriolvc.topicman.collection.domain;

import java.util.List;
import java.util.Optional;

/** Outbound port that stores collections with their templates. */
public interface CollectionRepository {

    /** A new collection id, never given before (ADR 0007). */
    CollectionId nextId();

    /** A new template id, never given before, for a template of any collection. */
    TemplateId nextTemplateId();

    /**
     * Inserts a new collection or updates an existing one with the same id, templates included.
     *
     * @throws CollectionNameTakenException if another collection has the same name
     */
    void save(Collection collection);

    Optional<Collection> findById(CollectionId id);

    /** All collections, ordered by name. */
    List<Collection> findAll();

    boolean existsByName(CollectionName name);

    void deleteById(CollectionId id);
}
