package com.oriolvc.topicman.messaging.application.model;

import com.oriolvc.topicman.messaging.domain.model.ConsumeSessionId;

public record StreamConsumeSessionCommand(ConsumeSessionId id, ConsumeSessionSink sink) {}
