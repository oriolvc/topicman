package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateConnectionUseCase {

    private final ConnectionRepository repository;
    private final Clock clock;

    public CreateConnectionUseCase(ConnectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ConnectionResult execute(CreateConnectionCommand command) {
        if (repository.existsByName(command.name())) {
            throw new ConnectionNameTakenException(command.name());
        }
        var connection = Connection.create(
                repository.nextId(),
                command.name(),
                command.brokerType(),
                command.address(),
                command.credentials(),
                clock.instant());
        repository.save(connection);
        return ConnectionResult.of(connection);
    }
}
