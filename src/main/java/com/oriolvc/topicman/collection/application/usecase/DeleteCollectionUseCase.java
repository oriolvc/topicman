package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.DeleteCollectionCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCollectionUseCase {

    private final CollectionRepository repository;

    public DeleteCollectionUseCase(CollectionRepository repository) {
        this.repository = repository;
    }

    /** Deletes the collection with its templates. */
    @Transactional
    public void execute(DeleteCollectionCommand command) {
        if (repository.findById(command.id()).isEmpty()) {
            throw new CollectionNotFoundException(command.id());
        }
        repository.deleteById(command.id());
    }
}
