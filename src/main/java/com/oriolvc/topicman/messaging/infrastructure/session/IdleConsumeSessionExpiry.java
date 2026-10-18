package com.oriolvc.topicman.messaging.infrastructure.session;

import com.oriolvc.topicman.messaging.application.ExpireIdleConsumeSessionsCommand;
import com.oriolvc.topicman.messaging.application.ExpireIdleConsumeSessionsUseCase;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** Runs the idle session expiry every {@code topicman.consume-sessions.expiry-check-interval}. */
@Component
class IdleConsumeSessionExpiry implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(IdleConsumeSessionExpiry.class);

    private final ExpireIdleConsumeSessionsUseCase expireIdleSessions;
    private final long intervalMillis;
    private @Nullable ScheduledExecutorService scheduler;

    IdleConsumeSessionExpiry(ExpireIdleConsumeSessionsUseCase expireIdleSessions, ConsumeSessionProperties properties) {
        this.expireIdleSessions = expireIdleSessions;
        this.intervalMillis = properties.expiryCheckInterval().toMillis();
    }

    @Override
    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("consume-session-expiry").factory());
        scheduler.scheduleWithFixedDelay(this::expire, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return scheduler != null;
    }

    private void expire() {
        // A failure must not cancel the schedule, which an uncaught exception would do.
        try {
            var result = expireIdleSessions.execute(new ExpireIdleConsumeSessionsCommand());
            if (result.expired() > 0) {
                log.info("Closed {} idle consume sessions", result.expired());
            }
        } catch (RuntimeException e) {
            log.error("Expiring idle consume sessions failed", e);
        }
    }
}
