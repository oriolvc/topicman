package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.CollectionResult;
import com.oriolvc.topicman.collection.application.model.GetCollectionCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCollectionUseCase {

    private final CollectionRepository repository;

    public GetCollectionUseCase(CollectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CollectionResult execute(GetCollectionCommand command) {
        return repository
                .findById(command.id())
                .map(CollectionResult::of)
                .orElseThrow(() -> new CollectionNotFoundException(command.id()));
    }
}
