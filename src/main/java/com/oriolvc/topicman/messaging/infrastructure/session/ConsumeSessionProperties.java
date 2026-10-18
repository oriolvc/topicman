package com.oriolvc.topicman.messaging.infrastructure.session;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param maxOpen how many consume sessions can be open at once
 * @param idleTimeout how long a session stays open while no client streams it
 * @param expiryCheckInterval how often idle sessions are looked for
 */
@ConfigurationProperties("topicman.consume-sessions")
public record ConsumeSessionProperties(
        @DefaultValue("20") int maxOpen,
        @DefaultValue("5m") Duration idleTimeout,
        @DefaultValue("10s") Duration expiryCheckInterval) {}
