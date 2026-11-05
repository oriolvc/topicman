package com.oriolvc.topicman.connection.application.usecase;

import com.oriolvc.topicman.connection.application.model.ConnectionResult;
import com.oriolvc.topicman.connection.application.model.UpdateConnectionCommand;
import com.oriolvc.topicman.connection.domain.exception.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.port.ConnectionRepository;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateConnectionUseCase {

    private final ConnectionRepository repository;
    private final Clock clock;

    public UpdateConnectionUseCase(ConnectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ConnectionResult execute(UpdateConnectionCommand command) {
        var connection =
                repository.findById(command.id()).orElseThrow(() -> new ConnectionNotFoundException(command.id()));
        var now = clock.instant();
        var name = command.name();
        if (name != null && !name.equals(connection.name()) && repository.existsByName(name)) {
            throw new ConnectionNameTakenException(name);
        }
        // Credentials and TLS settings first: they are the only changes a business rule can reject,
        // so a rejected update changes nothing.
        if (command.credentials() != null) {
            connection.changeCredentials(command.credentials(), now);
        }
        if (command.tls() != null) {
            connection.changeTls(command.tls(), now);
        }
        if (command.address() != null) {
            connection.changeAddress(command.address(), now);
        }
        if (name != null) {
            connection.rename(name, now);
        }
        repository.save(connection);
        return ConnectionResult.of(connection);
    }
}
