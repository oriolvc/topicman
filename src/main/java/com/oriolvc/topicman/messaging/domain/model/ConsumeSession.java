package com.oriolvc.topicman.messaging.domain.model;

import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionClosedException;
import com.oriolvc.topicman.messaging.domain.exception.InvalidDestinationException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An ephemeral, read-only session that streams messages from one destination. It never changes
 * the state of real consumers (ADR 0003), and it is not persisted: it lives as long as a client is
 * connected, up to the idle timeout.
 */
public final class ConsumeSession {

    private final ConsumeSessionId id;
    private final ConnectionId connectionId;
    private final BrokerType brokerType;
    private final Destination destination;
    private final StartPosition startPosition;
    private final Instant openedAt;
    private Instant lastActivityAt;
    private @Nullable Instant closedAt;

    private ConsumeSession(
            ConsumeSessionId id,
            ConnectionId connectionId,
            BrokerType brokerType,
            Destination destination,
            StartPosition startPosition,
            Instant openedAt) {
        this.id = id;
        this.connectionId = connectionId;
        this.brokerType = brokerType;
        this.destination = destination;
        this.startPosition = startPosition;
        this.openedAt = openedAt;
        this.lastActivityAt = openedAt;
    }

    public static ConsumeSession open(
            ConnectionId connectionId,
            BrokerType brokerType,
            Destination destination,
            StartPosition startPosition,
            Instant now) {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(brokerType, "brokerType");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(startPosition, "startPosition");
        Objects.requireNonNull(now, "now");
        if (!destination.isSupportedBy(brokerType)) {
            throw new InvalidDestinationException(
                    destination.getClass().getSimpleName() + " destinations cannot be read from " + brokerType);
        }
        return new ConsumeSession(ConsumeSessionId.newId(), connectionId, brokerType, destination, startPosition, now);
    }

    /** Records that the session is in use, for example a message was delivered or the client polled. */
    public void recordActivity(Instant at) {
        Objects.requireNonNull(at, "at");
        if (!isOpen()) {
            throw new ConsumeSessionClosedException(id);
        }
        if (at.isAfter(lastActivityAt)) {
            lastActivityAt = at;
        }
    }

    /** Closes the session. Closing an already closed session keeps the original close time. */
    public void close(Instant at) {
        Objects.requireNonNull(at, "at");
        if (closedAt == null) {
            closedAt = at;
        }
    }

    /** Whether the session saw no activity for at least {@code timeout}. A closed session is not idle. */
    public boolean isIdle(Duration timeout, Instant now) {
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(now, "now");
        return isOpen() && !lastActivityAt.plus(timeout).isAfter(now);
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    public ConsumeSessionId id() {
        return id;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }

    public BrokerType brokerType() {
        return brokerType;
    }

    public Destination destination() {
        return destination;
    }

    public StartPosition startPosition() {
        return startPosition;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }

    public @Nullable Instant closedAt() {
        return closedAt;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ConsumeSession session && id.equals(session.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
