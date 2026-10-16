package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A saved connection to one broker. The broker type is fixed at creation: the address format and
 * the authentication options depend on it, so switching brokers means creating a new connection.
 */
public final class Connection {

    private final ConnectionId id;
    private final BrokerType brokerType;
    private final Instant createdAt;
    private ConnectionName name;
    private BrokerAddress address;
    private Instant updatedAt;

    private Connection(
            ConnectionId id,
            ConnectionName name,
            BrokerType brokerType,
            BrokerAddress address,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.brokerType = Objects.requireNonNull(brokerType, "brokerType");
        this.address = Objects.requireNonNull(address, "address");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Connection create(ConnectionName name, BrokerType brokerType, BrokerAddress address, Instant now) {
        return new Connection(ConnectionId.newId(), name, brokerType, address, now, now);
    }

    public static Connection rehydrate(
            ConnectionId id,
            ConnectionName name,
            BrokerType brokerType,
            BrokerAddress address,
            Instant createdAt,
            Instant updatedAt) {
        return new Connection(id, name, brokerType, address, createdAt, updatedAt);
    }

    public void rename(ConnectionName newName, Instant now) {
        name = Objects.requireNonNull(newName, "newName");
        touch(now);
    }

    public void changeAddress(BrokerAddress newAddress, Instant now) {
        address = Objects.requireNonNull(newAddress, "newAddress");
        touch(now);
    }

    private void touch(Instant now) {
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public ConnectionId id() {
        return id;
    }

    public ConnectionName name() {
        return name;
    }

    public BrokerType brokerType() {
        return brokerType;
    }

    public BrokerAddress address() {
        return address;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Connection connection && id.equals(connection.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
