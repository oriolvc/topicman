package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.shared.domain.ConnectionId;

public record ProduceMessageCommand(ConnectionId connectionId, Destination destination, Message message, int copies) {}
