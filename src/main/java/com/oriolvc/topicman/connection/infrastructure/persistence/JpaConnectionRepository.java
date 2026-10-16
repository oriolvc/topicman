package com.oriolvc.topicman.connection.infrastructure.persistence;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.Connection;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.ConnectionRepository;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaConnectionRepository implements ConnectionRepository {

    private final ConnectionJpaRepository jpaRepository;

    JpaConnectionRepository(ConnectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Connection connection) {
        // Update the managed entity when it exists, so its @Version is kept and checked.
        var entity = jpaRepository
                .findById(connection.id().value())
                .map(existing -> {
                    existing.update(
                            connection.name().value(), connection.address().value(), connection.updatedAt());
                    return existing;
                })
                .orElseGet(() -> new ConnectionEntity(
                        connection.id().value(),
                        connection.name().value(),
                        connection.brokerType(),
                        connection.address().value(),
                        connection.createdAt(),
                        connection.updatedAt()));
        jpaRepository.save(entity);
    }

    @Override
    public Optional<Connection> findById(ConnectionId id) {
        return jpaRepository.findById(id.value()).map(JpaConnectionRepository::toDomain);
    }

    @Override
    public List<Connection> findAll() {
        return jpaRepository.findAllByOrderByNameAsc().stream()
                .map(JpaConnectionRepository::toDomain)
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

    private static Connection toDomain(ConnectionEntity entity) {
        return Connection.rehydrate(
                new ConnectionId(entity.id()),
                new ConnectionName(entity.name()),
                entity.brokerType(),
                new BrokerAddress(entity.address()),
                entity.createdAt(),
                entity.updatedAt());
    }
}
