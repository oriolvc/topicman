package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetConnectionUseCase {

    private final ConnectionRepository repository;

    public GetConnectionUseCase(ConnectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ConnectionResult execute(GetConnectionCommand command) {
        return repository
                .findById(command.id())
                .map(ConnectionResult::of)
                .orElseThrow(() -> new ConnectionNotFoundException(command.id()));
    }
}
