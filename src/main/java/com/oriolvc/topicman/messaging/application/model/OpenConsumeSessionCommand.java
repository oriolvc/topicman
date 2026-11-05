package com.oriolvc.topicman.messaging.application.model;

import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.shared.domain.ConnectionId;

public record OpenConsumeSessionCommand(
        ConnectionId connectionId, Destination destination, StartPosition startPosition) {}
