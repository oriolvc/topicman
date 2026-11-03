package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.Collection;
import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
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
        var collection = Collection.create(command.name(), clock.instant());
        repository.save(collection);
        return CollectionResult.of(collection);
    }
}
