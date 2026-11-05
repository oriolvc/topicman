package com.oriolvc.topicman.messaging.application.helper;

import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionNotFoundException;
import com.oriolvc.topicman.messaging.domain.model.ConsumeSessionId;
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
    public synchronized boolean registerWithin(ActiveSession session, int maxOpen) {
        if (sessions.size() >= maxOpen) {
            return false;
        }
        sessions.put(session.session().id(), session);
        return true;
    }

    public int size() {
        return sessions.size();
    }

    public List<ActiveSession> all() {
        return List.copyOf(sessions.values());
    }

    public boolean contains(ConsumeSessionId id) {
        return sessions.containsKey(id);
    }

    public ActiveSession get(ConsumeSessionId id) {
        var session = sessions.get(id);
        if (session == null) {
            throw new ConsumeSessionNotFoundException(id);
        }
        return session;
    }

    public Optional<ActiveSession> remove(ConsumeSessionId id) {
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
