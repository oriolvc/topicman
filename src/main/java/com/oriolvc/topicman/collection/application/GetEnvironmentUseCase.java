package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.EnvironmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetEnvironmentUseCase {

    private final EnvironmentRepository repository;

    public GetEnvironmentUseCase(EnvironmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public EnvironmentResult execute(GetEnvironmentCommand command) {
        return repository
                .findById(command.id())
                .map(EnvironmentResult::of)
                .orElseThrow(() -> new EnvironmentNotFoundException(command.id()));
    }
}
