package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.Collection;
import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
import com.oriolvc.topicman.collection.domain.SupportedBrokers;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a collection from an imported file. Each template is checked as if it were added on its
 * own, and nothing is saved unless all of them are accepted.
 */
@Service
public class ImportCollectionUseCase {

    private final CollectionRepository repository;
    private final SupportedBrokers supportedBrokers;
    private final Clock clock;

    public ImportCollectionUseCase(CollectionRepository repository, SupportedBrokers supportedBrokers, Clock clock) {
        this.repository = repository;
        this.supportedBrokers = supportedBrokers;
        this.clock = clock;
    }

    @Transactional
    public CollectionResult execute(ImportCollectionCommand command) {
        if (repository.existsByName(command.name())) {
            throw new CollectionNameTakenException(command.name());
        }
        var templates = command.templates().stream()
                .map(content -> content.toNewTemplate(repository.nextTemplateId()))
                .toList();
        templates.forEach(supportedBrokers::check);
        var collection = Collection.fromFile(repository.nextId(), command.name(), templates, clock.instant());
        repository.save(collection);
        return CollectionResult.of(collection);
    }
}
