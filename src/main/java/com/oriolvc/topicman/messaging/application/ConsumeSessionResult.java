package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConsumeSession;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.Instant;

public record ConsumeSessionResult(
        ConsumeSessionId id,
        ConnectionId connectionId,
        Destination destination,
        StartPosition startPosition,
        Instant openedAt) {

    static ConsumeSessionResult of(ConsumeSession session) {
        return new ConsumeSessionResult(
                session.id(),
                session.connectionId(),
                session.destination(),
                session.startPosition(),
                session.openedAt());
    }
}
