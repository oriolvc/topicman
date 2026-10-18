package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionInUseException;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** The open consume sessions. They are ephemeral (ADR 0003), so they live only in memory. */
@Component
public class ConsumeSessionRegistry implements AutoCloseable {

    private final Map<ConsumeSessionId, ActiveSession> sessions = new ConcurrentHashMap<>();

    /** @return false, without registering, if {@code maxOpen} sessions are already open */
    synchronized boolean registerWithin(ActiveSession session, int maxOpen) {
        if (sessions.size() >= maxOpen) {
            return false;
        }
        sessions.put(session.session().id(), session);
        return true;
    }

    int size() {
        return sessions.size();
    }

    List<ActiveSession> all() {
        return List.copyOf(sessions.values());
    }

    boolean contains(ConsumeSessionId id) {
        return sessions.containsKey(id);
    }

    /** Reserves the session for one streaming client. */
    ActiveSession claim(ConsumeSessionId id, Instant now) {
        var session = sessions.get(id);
        if (session == null) {
            throw new ConsumeSessionNotFoundException(id);
        }
        if (!session.tryClaim(now)) {
            throw new ConsumeSessionInUseException(id);
        }
        return session;
    }

    Optional<ActiveSession> remove(ConsumeSessionId id) {
        return Optional.ofNullable(sessions.remove(id));
    }

    /** Closes every session on shutdown; sessions being streamed are closed by their stream. */
    @Override
    public void close() {
        sessions.keySet()
                .forEach(id -> remove(id).ifPresent(session -> {
                    if (session.requestClose()) {
                        session.stream().close();
                    }
                }));
    }
}
