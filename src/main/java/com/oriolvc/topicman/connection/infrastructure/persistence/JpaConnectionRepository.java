package com.oriolvc.topicman.connection.infrastructure.persistence;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaConnectionRepository implements ConnectionRepository {

    // Declared in V1__create_schema.sql
    private static final String NAME_UNIQUE_CONSTRAINT = "broker_connection_name_unique";

    private final ConnectionJpaRepository jpaRepository;
    private final CredentialsCodec credentialsCodec;

    JpaConnectionRepository(ConnectionJpaRepository jpaRepository, CredentialsCodec credentialsCodec) {
        this.jpaRepository = jpaRepository;
        this.credentialsCodec = credentialsCodec;
    }

    @Override
    public ConnectionId nextId() {
        return new ConnectionId(jpaRepository.nextId());
    }

    @Override
    public void save(Connection connection) {
        var credentials = credentialsCodec.encode(connection.id(), connection.credentials());
        var tls = StoredTls.of(connection.tls());
        // Update the managed entity when it exists, so its @Version is kept and checked.
        var entity = jpaRepository
                .findById(connection.id().value())
                .map(existing -> {
                    existing.update(
                            connection.name().value(),
                            connection.address().value(),
                            credentials,
                            tls,
                            connection.updatedAt());
                    return existing;
                })
                .orElseGet(() -> new ConnectionEntity(
                        connection.id().value(),
                        connection.name().value(),
                        connection.brokerType(),
                        connection.address().value(),
                        credentials,
                        tls,
                        connection.createdAt(),
                        connection.updatedAt()));
        try {
            // Flush now, so a name taken by a concurrent request fails here as a domain error
            // instead of at commit.
            jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (isNameUniqueViolation(e)) {
                throw new ConnectionNameTakenException(connection.name());
            }
            throw e;
        }
    }

    private static boolean isNameUniqueViolation(DataIntegrityViolationException e) {
        return e.getMostSpecificCause().getMessage() != null
                && e.getMostSpecificCause().getMessage().contains(NAME_UNIQUE_CONSTRAINT);
    }

    @Override
    public Optional<Connection> findById(ConnectionId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Connection> findAll() {
        return jpaRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(ConnectionName name) {
        return jpaRepository.existsByName(name.value());
    }

    @Override
    public void deleteById(ConnectionId id) {
        jpaRepository.deleteById(id.value());
    }

    private Connection toDomain(ConnectionEntity entity) {
        var id = new ConnectionId(entity.id());
        return Connection.rehydrate(
                id,
                new ConnectionName(entity.name()),
                entity.brokerType(),
                new BrokerAddress(entity.address()),
                credentialsCodec.decode(id, entity.credentials()),
                entity.tls().toDomain(),
                entity.createdAt(),
                entity.updatedAt());
    }
}
