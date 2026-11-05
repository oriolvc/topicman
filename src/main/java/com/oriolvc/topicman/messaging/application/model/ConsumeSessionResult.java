package com.oriolvc.topicman.messaging.application.model;

import com.oriolvc.topicman.messaging.domain.model.ConsumeSession;
import com.oriolvc.topicman.messaging.domain.model.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.Instant;

public record ConsumeSessionResult(
        ConsumeSessionId id,
        ConnectionId connectionId,
        Destination destination,
        StartPosition startPosition,
        Instant openedAt) {

    public static ConsumeSessionResult of(ConsumeSession session) {
        return new ConsumeSessionResult(
                session.id(),
                session.connectionId(),
                session.destination(),
                session.startPosition(),
                session.openedAt());
    }
}
