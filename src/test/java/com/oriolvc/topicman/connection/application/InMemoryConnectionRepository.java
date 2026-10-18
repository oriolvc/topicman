package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Keeps the saved instances themselves, so tests can see what a use case stored. */
class InMemoryConnectionRepository implements ConnectionRepository {

    private final Map<ConnectionId, Connection> connections = new LinkedHashMap<>();

    @Override
    public void save(Connection connection) {
        connections.put(connection.id(), connection);
    }

    @Override
    public Optional<Connection> findById(ConnectionId id) {
        return Optional.ofNullable(connections.get(id));
    }

    @Override
    public List<Connection> findAll() {
        return connections.values().stream()
                .sorted(Comparator.comparing(connection -> connection.name().value()))
                .toList();
    }

    @Override
    public boolean existsByName(ConnectionName name) {
        return connections.values().stream()
                .anyMatch(connection -> connection.name().equals(name));
    }

    @Override
    public void deleteById(ConnectionId id) {
        connections.remove(id);
    }
}
