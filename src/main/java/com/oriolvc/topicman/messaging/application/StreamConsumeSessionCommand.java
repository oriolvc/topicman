package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;

public record StreamConsumeSessionCommand(ConsumeSessionId id, ConsumeSessionSink sink) {}
