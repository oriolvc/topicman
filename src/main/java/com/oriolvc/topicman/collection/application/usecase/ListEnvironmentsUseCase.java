package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.EnvironmentResult;
import com.oriolvc.topicman.collection.application.model.ListEnvironmentsCommand;
import com.oriolvc.topicman.collection.domain.port.EnvironmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListEnvironmentsUseCase {

    private final EnvironmentRepository repository;

    public ListEnvironmentsUseCase(EnvironmentRepository repository) {
        this.repository = repository;
    }

    /** Every environment with its variables, ordered by name. */
    @Transactional(readOnly = true)
    public List<EnvironmentResult> execute(ListEnvironmentsCommand command) {
        return repository.findAll().stream().map(EnvironmentResult::of).toList();
    }
}
