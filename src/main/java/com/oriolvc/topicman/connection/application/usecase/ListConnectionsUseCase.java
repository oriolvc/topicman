package com.oriolvc.topicman.connection.application.usecase;

import com.oriolvc.topicman.connection.application.model.ConnectionResult;
import com.oriolvc.topicman.connection.application.model.ListConnectionsCommand;
import com.oriolvc.topicman.connection.domain.port.ConnectionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListConnectionsUseCase {

    private final ConnectionRepository repository;

    public ListConnectionsUseCase(ConnectionRepository repository) {
        this.repository = repository;
    }

    /** All connections, ordered by name. */
    @Transactional(readOnly = true)
    public List<ConnectionResult> execute(ListConnectionsCommand command) {
        return repository.findAll().stream().map(ConnectionResult::of).toList();
    }
}
