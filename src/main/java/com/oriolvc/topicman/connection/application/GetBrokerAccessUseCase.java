package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives the messaging context what it needs to reach a broker, decrypted credentials included: the
 * only use case that returns secrets (ADR 0008). No controller calls it, which ArchUnit enforces,
 * so secrets never leave the server.
 */
@Service
public class GetBrokerAccessUseCase {

    private final ConnectionRepository repository;

    public GetBrokerAccessUseCase(ConnectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BrokerAccessResult execute(GetBrokerAccessCommand command) {
        return repository
                .findById(command.id())
                .map(BrokerAccessResult::of)
                .orElseThrow(() -> new ConnectionNotFoundException(command.id()));
    }
}
