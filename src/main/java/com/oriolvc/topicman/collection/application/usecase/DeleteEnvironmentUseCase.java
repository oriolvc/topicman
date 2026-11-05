package com.oriolvc.topicman.collection.application.usecase;

import com.oriolvc.topicman.collection.application.model.DeleteEnvironmentCommand;
import com.oriolvc.topicman.collection.domain.exception.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.port.EnvironmentRepository;
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
