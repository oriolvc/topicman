package com.oriolvc.topicman.collection.infrastructure.persistence;

import com.oriolvc.topicman.collection.domain.Environment;
import com.oriolvc.topicman.collection.domain.EnvironmentId;
import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.domain.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.EnvironmentRepository;
import com.oriolvc.topicman.collection.domain.VariableName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaEnvironmentRepository implements EnvironmentRepository {

    // Declared in V3__create_collection_and_environment_tables.sql
    private static final String NAME_UNIQUE_CONSTRAINT = "environment_name_unique";

    private final EnvironmentJpaRepository jpaRepository;

    JpaEnvironmentRepository(EnvironmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Environment environment) {
        var variables = environment.variables().entrySet().stream()
                .map(variable -> new NameValueEmbeddable(variable.getKey().value(), variable.getValue()))
                .toList();
        // Update the managed entity when it exists, so its @Version is kept and checked.
        var entity = jpaRepository
                .findById(environment.id().value())
                .map(existing -> {
                    existing.update(environment.name().value(), variables, environment.updatedAt());
                    return existing;
                })
                .orElseGet(() -> new EnvironmentEntity(
                        environment.id().value(),
                        environment.name().value(),
                        variables,
                        environment.createdAt(),
                        environment.updatedAt()));
        try {
            // Flush now, so a name taken by a concurrent request fails here as a domain error.
            jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (Constraints.violates(e, NAME_UNIQUE_CONSTRAINT)) {
                throw new EnvironmentNameTakenException(environment.name());
            }
            throw e;
        }
    }

    @Override
    public Optional<Environment> findById(EnvironmentId id) {
        return jpaRepository.findById(id.value()).map(JpaEnvironmentRepository::toDomain);
    }

    @Override
    public List<Environment> findAll() {
        return jpaRepository.findAllByOrderByNameAsc().stream()
                .map(JpaEnvironmentRepository::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(EnvironmentName name) {
        return jpaRepository.existsByName(name.value());
    }

    @Override
    public void deleteById(EnvironmentId id) {
        jpaRepository.deleteById(id.value());
    }

    private static Environment toDomain(EnvironmentEntity entity) {
        var variables = new LinkedHashMap<VariableName, String>();
        entity.variables().forEach(variable -> variables.put(new VariableName(variable.name()), variable.value()));
        return Environment.rehydrate(
                new EnvironmentId(entity.id()),
                new EnvironmentName(entity.name()),
                variables,
                entity.createdAt(),
                entity.updatedAt());
    }
}
