package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.CollectionResult;
import com.oriolvc.topicman.collection.application.model.CreateCollectionCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.model.Collection;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCollectionUseCase {

    private final CollectionRepository repository;
    private final Clock clock;

    public CreateCollectionUseCase(CollectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CollectionResult execute(CreateCollectionCommand command) {
        if (repository.existsByName(command.name())) {
            throw new CollectionNameTakenException(command.name());
        }
        var collection = Collection.create(repository.nextId(), command.name(), clock.instant());
        repository.save(collection);
        return CollectionResult.of(collection);
    }
}
