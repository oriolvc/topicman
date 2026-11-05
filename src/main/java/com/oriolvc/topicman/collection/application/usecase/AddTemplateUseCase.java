package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.AddTemplateCommand;
import com.oriolvc.topicman.collection.application.model.TemplateResult;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import com.oriolvc.topicman.collection.domain.port.SupportedBrokers;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Appends a template, once the messaging context confirms it fits its broker (ADR 0006). */
@Service
public class AddTemplateUseCase {

    private final CollectionRepository repository;
    private final SupportedBrokers supportedBrokers;
    private final Clock clock;

    public AddTemplateUseCase(CollectionRepository repository, SupportedBrokers supportedBrokers, Clock clock) {
        this.repository = repository;
        this.supportedBrokers = supportedBrokers;
        this.clock = clock;
    }

    @Transactional
    public TemplateResult execute(AddTemplateCommand command) {
        var collection = repository
                .findById(command.collectionId())
                .orElseThrow(() -> new CollectionNotFoundException(command.collectionId()));
        var template = command.content().toNewTemplate(repository.nextTemplateId());
        supportedBrokers.check(template);
        collection.addTemplate(template, clock.instant());
        repository.save(collection);
        return TemplateResult.of(template);
    }
}
