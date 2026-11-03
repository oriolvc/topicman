package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
import com.oriolvc.topicman.collection.domain.SupportedBrokers;
import com.oriolvc.topicman.collection.domain.TemplateNotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Replaces a template's content, keeping its id and its place in the collection. */
@Service
public class ReplaceTemplateUseCase {

    private final CollectionRepository repository;
    private final SupportedBrokers supportedBrokers;
    private final Clock clock;

    public ReplaceTemplateUseCase(CollectionRepository repository, SupportedBrokers supportedBrokers, Clock clock) {
        this.repository = repository;
        this.supportedBrokers = supportedBrokers;
        this.clock = clock;
    }

    @Transactional
    public TemplateResult execute(ReplaceTemplateCommand command) {
        var collection = repository
                .findById(command.collectionId())
                .orElseThrow(() -> new CollectionNotFoundException(command.collectionId()));
        var existing = collection
                .template(command.templateId())
                .orElseThrow(() -> new TemplateNotFoundException(command.templateId()));
        var replacement = command.content().applyTo(existing);
        supportedBrokers.check(replacement);
        collection.replaceTemplate(replacement, clock.instant());
        repository.save(collection);
        return TemplateResult.of(replacement);
    }
}
