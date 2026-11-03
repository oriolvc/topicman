package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenameCollectionUseCase {

    private final CollectionRepository repository;
    private final Clock clock;

    public RenameCollectionUseCase(CollectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CollectionResult execute(RenameCollectionCommand command) {
        var collection =
                repository.findById(command.id()).orElseThrow(() -> new CollectionNotFoundException(command.id()));
        if (!command.name().equals(collection.name()) && repository.existsByName(command.name())) {
            throw new CollectionNameTakenException(command.name());
        }
        collection.rename(command.name(), clock.instant());
        repository.save(collection);
        return CollectionResult.of(collection);
    }
}
