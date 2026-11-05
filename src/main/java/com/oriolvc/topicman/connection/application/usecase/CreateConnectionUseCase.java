package com.oriolvc.topicman.connection.application.usecase;

import com.oriolvc.topicman.connection.application.model.ConnectionResult;
import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.domain.exception.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.model.Connection;
import com.oriolvc.topicman.connection.domain.port.ConnectionRepository;
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
                command.tls(),
                clock.instant());
        repository.save(connection);
        return ConnectionResult.of(connection);
    }
}
