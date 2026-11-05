package com.oriolvc.topicman.messaging.application.model;

import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.Message;
import com.oriolvc.topicman.shared.domain.ConnectionId;

public record ProduceMessageCommand(ConnectionId connectionId, Destination destination, Message message, int copies) {}
