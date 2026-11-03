package com.oriolvc.topicman.collection.domain;

import java.util.List;
import java.util.Optional;

/** Outbound port that stores environments with their variables. */
public interface EnvironmentRepository {

    /**
     * Inserts a new environment or updates an existing one with the same id.
     *
     * @throws EnvironmentNameTakenException if another environment has the same name
     */
    void save(Environment environment);

    Optional<Environment> findById(EnvironmentId id);

    /** All environments, ordered by name. */
    List<Environment> findAll();

    boolean existsByName(EnvironmentName name);

    void deleteById(EnvironmentId id);
}
