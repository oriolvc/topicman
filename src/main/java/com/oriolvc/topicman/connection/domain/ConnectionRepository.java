package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.util.List;
import java.util.Optional;

/** Outbound port that stores connections. */
public interface ConnectionRepository {

    /** Inserts a new connection or updates an existing one with the same id. */
    void save(Connection connection);

    Optional<Connection> findById(ConnectionId id);

    /** All connections, ordered by name. */
    List<Connection> findAll();

    boolean existsByName(ConnectionName name);

    void deleteById(ConnectionId id);
}
