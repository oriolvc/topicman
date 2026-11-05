package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.domain.exception.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.model.Environment;
import com.oriolvc.topicman.collection.domain.model.EnvironmentId;
import com.oriolvc.topicman.collection.domain.model.EnvironmentName;
import com.oriolvc.topicman.collection.domain.port.EnvironmentRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores rehydrated copies, so a test only sees what a use case saved; ids come from a counter. */
class InMemoryEnvironmentRepository implements EnvironmentRepository {

    private final Map<EnvironmentId, Environment> environments = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public EnvironmentId nextId() {
        return new EnvironmentId(nextId++);
    }

    @Override
    public void save(Environment environment) {
        var taken = environments.values().stream()
                .anyMatch(other ->
                        other.name().equals(environment.name()) && !other.id().equals(environment.id()));
        if (taken) {
            throw new EnvironmentNameTakenException(environment.name());
        }
        environments.put(environment.id(), copyOf(environment));
    }

    @Override
    public Optional<Environment> findById(EnvironmentId id) {
        return Optional.ofNullable(environments.get(id)).map(InMemoryEnvironmentRepository::copyOf);
    }

    @Override
    public List<Environment> findAll() {
        return environments.values().stream()
                .sorted(Comparator.comparing(environment -> environment.name().value()))
                .map(InMemoryEnvironmentRepository::copyOf)
                .toList();
    }

    @Override
    public boolean existsByName(EnvironmentName name) {
        return environments.values().stream()
                .anyMatch(environment -> environment.name().equals(name));
    }

    @Override
    public void deleteById(EnvironmentId id) {
        environments.remove(id);
    }

    private static Environment copyOf(Environment environment) {
        return Environment.rehydrate(
                environment.id(),
                environment.name(),
                environment.variables(),
                environment.createdAt(),
                environment.updatedAt());
    }
}
