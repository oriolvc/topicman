package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.ConnectionId;

public record OpenConsumeSessionCommand(
        ConnectionId connectionId, Destination destination, StartPosition startPosition) {}
