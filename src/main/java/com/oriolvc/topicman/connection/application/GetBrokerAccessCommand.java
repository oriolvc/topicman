package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.shared.domain.ConnectionId;

public record GetBrokerAccessCommand(ConnectionId id) {}
