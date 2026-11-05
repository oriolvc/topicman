package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.DeleteTemplateCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTemplateUseCase {

    private final CollectionRepository repository;
    private final Clock clock;

    public DeleteTemplateUseCase(CollectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void execute(DeleteTemplateCommand command) {
        var collection = repository
                .findById(command.collectionId())
                .orElseThrow(() -> new CollectionNotFoundException(command.collectionId()));
        collection.removeTemplate(command.templateId(), clock.instant());
        repository.save(collection);
    }
}
