package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.util.Objects;

public record TestConnectionCommand(ConnectionId connectionId) {

    public TestConnectionCommand {
        Objects.requireNonNull(connectionId, "connectionId");
    }
}
