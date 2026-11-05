package com.oriolvc.topicman.connection.domain.port;

import com.oriolvc.topicman.connection.domain.exception.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.model.Connection;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.util.List;
import java.util.Optional;

/** Outbound port that stores connections. */
public interface ConnectionRepository {

    /** A new connection id, never given before (ADR 0007). */
    ConnectionId nextId();

    /**
     * Inserts a new connection or updates an existing one with the same id.
     *
     * @throws ConnectionNameTakenException if another connection has the same name
     */
    void save(Connection connection);

    Optional<Connection> findById(ConnectionId id);

    /** All connections, ordered by name. */
    List<Connection> findAll();

    boolean existsByName(ConnectionName name);

    void deleteById(ConnectionId id);
}
