package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.CollectionSummaryResult;
import com.oriolvc.topicman.collection.application.model.ListCollectionsCommand;
import com.oriolvc.topicman.collection.domain.port.CollectionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCollectionsUseCase {

    private final CollectionRepository repository;

    public ListCollectionsUseCase(CollectionRepository repository) {
        this.repository = repository;
    }

    /** Every collection, ordered by name, without its templates. */
    @Transactional(readOnly = true)
    public List<CollectionSummaryResult> execute(ListCollectionsCommand command) {
        return repository.findAll().stream().map(CollectionSummaryResult::of).toList();
    }
}
