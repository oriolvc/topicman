package com.oriolvc.topicman.collection.infrastructure.persistence;

import com.oriolvc.topicman.collection.domain.Collection;
import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
import com.oriolvc.topicman.collection.domain.MessageTemplate;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateId;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaCollectionRepository implements CollectionRepository {

    // Declared in V1__create_schema.sql
    private static final String NAME_UNIQUE_CONSTRAINT = "collection_name_unique";

    private final CollectionJpaRepository jpaRepository;

    JpaCollectionRepository(CollectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CollectionId nextId() {
        return new CollectionId(jpaRepository.nextId());
    }

    @Override
    public TemplateId nextTemplateId() {
        return new TemplateId(jpaRepository.nextTemplateId());
    }

    @Override
    public void save(Collection collection) {
        var templates = collection.templates().stream()
                .map(JpaCollectionRepository::toEntity)
                .toList();
        // Update the managed entity when it exists, so its @Version is kept and checked.
        var entity = jpaRepository
                .findById(collection.id().value())
                .map(existing -> {
                    existing.update(collection.name().value(), templates, collection.updatedAt());
                    return existing;
                })
                .orElseGet(() -> new CollectionEntity(
                        collection.id().value(),
                        collection.name().value(),
                        templates,
                        collection.createdAt(),
                        collection.updatedAt()));
        try {
            // Flush now, so a name taken by a concurrent request fails here as a domain error.
            jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (Constraints.violates(e, NAME_UNIQUE_CONSTRAINT)) {
                throw new CollectionNameTakenException(collection.name());
            }
            throw e;
        }
    }

    @Override
    public Optional<Collection> findById(CollectionId id) {
        return jpaRepository.findById(id.value()).map(JpaCollectionRepository::toDomain);
    }

    @Override
    public List<Collection> findAll() {
        return jpaRepository.findAllByOrderByNameAsc().stream()
                .map(JpaCollectionRepository::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(CollectionName name) {
        return jpaRepository.existsByName(name.value());
    }

    @Override
    public void deleteById(CollectionId id) {
        jpaRepository.deleteById(id.value());
    }

    private static TemplateEntity toEntity(MessageTemplate template) {
        var headers = template.headers().entrySet().stream()
                .map(header -> new NameValueEmbeddable(header.getKey(), header.getValue()))
                .toList();
        var destination = template.destination();
        String destinationName;
        @Nullable String routingKey = null;
        switch (destination) {
            case TemplateDestination.Topic(var name) -> destinationName = name;
            case TemplateDestination.Exchange(var name, var key) -> {
                destinationName = name;
                routingKey = key;
            }
        }
        return new TemplateEntity(
                template.id().value(),
                template.name().value(),
                template.brokerType(),
                destination.kind(),
                destinationName,
                routingKey,
                template.key(),
                headers,
                template.payload().format(),
                template.payload().content());
    }

    private static Collection toDomain(CollectionEntity entity) {
        return Collection.rehydrate(
                new CollectionId(entity.id()),
                new CollectionName(entity.name()),
                entity.templates().stream()
                        .map(JpaCollectionRepository::toDomain)
                        .toList(),
                entity.createdAt(),
                entity.updatedAt());
    }

    private static MessageTemplate toDomain(TemplateEntity entity) {
        var headers = new LinkedHashMap<String, String>();
        entity.headers().forEach(header -> headers.put(header.name(), header.value()));
        var destination = switch (entity.destinationKind()) {
            case TOPIC -> TemplateDestination.topic(entity.destinationName());
            case EXCHANGE ->
                TemplateDestination.exchange(
                        entity.destinationName(), Objects.requireNonNullElse(entity.routingKey(), ""));
        };
        return new MessageTemplate(
                new TemplateId(entity.id()),
                new TemplateName(entity.name()),
                entity.brokerType(),
                destination,
                entity.messageKey(),
                headers,
                new TemplatePayload(entity.payloadFormat(), entity.payloadContent()));
    }
}
