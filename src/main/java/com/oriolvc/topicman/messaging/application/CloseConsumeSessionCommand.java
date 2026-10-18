package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;

public record CloseConsumeSessionCommand(ConsumeSessionId id) {}
