package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.EnvironmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteEnvironmentUseCase {

    private final EnvironmentRepository repository;

    public DeleteEnvironmentUseCase(EnvironmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(DeleteEnvironmentCommand command) {
        if (repository.findById(command.id()).isEmpty()) {
            throw new EnvironmentNotFoundException(command.id());
        }
        repository.deleteById(command.id());
    }
}
