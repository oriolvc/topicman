package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.ReplaceTemplateCommand;
import com.oriolvc.topicman.collection.application.model.TemplateResult;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.exception.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import com.oriolvc.topicman.collection.domain.port.SupportedBrokers;
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
