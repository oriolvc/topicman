package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteConnectionUseCase {

    private final ConnectionRepository repository;

    public DeleteConnectionUseCase(ConnectionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(DeleteConnectionCommand command) {
        if (repository.findById(command.id()).isEmpty()) {
            throw new ConnectionNotFoundException(command.id());
        }
        repository.deleteById(command.id());
    }
}
