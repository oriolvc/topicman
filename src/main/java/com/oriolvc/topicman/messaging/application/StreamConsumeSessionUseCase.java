package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class StreamConsumeSessionUseCase {

    static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

    private static final Logger log = LoggerFactory.getLogger(StreamConsumeSessionUseCase.class);

    private final ConsumeSessionRegistry registry;
    private final Executor executor;
    private final Clock clock;

    public StreamConsumeSessionUseCase(
            ConsumeSessionRegistry registry, @Qualifier("consumeSessionExecutor") Executor executor, Clock clock) {
        this.registry = registry;
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * Claims the session for this client, failing right away if it is unknown or already streamed,
     * then streams it on the executor until the client leaves or the session closes. Reading pauses
     * while no client is connected, so the next client continues where this one stopped.
     */
    public void execute(StreamConsumeSessionCommand command) {
        var active = registry.claim(command.id(), clock.instant());
        try {
            executor.execute(() -> stream(active, command.sink()));
        } catch (RuntimeException e) {
            release(active);
            throw e;
        }
    }

    private void stream(ActiveSession active, ConsumeSessionSink sink) {
        try {
            var lastSent = clock.instant();
            while (!active.isCloseRequested() && sink.isConnected()) {
                if (!deliverUndelivered(active, sink)) {
                    return;
                }
                if (active.undelivered().isEmpty()) {
                    active.undelivered().addAll(active.stream().poll(POLL_TIMEOUT));
                }
                var now = clock.instant();
                if (!active.undelivered().isEmpty()) {
                    lastSent = now;
                } else if (!Duration.between(lastSent, now)
                        .minus(HEARTBEAT_INTERVAL)
                        .isNegative()) {
                    if (!sink.heartbeat()) {
                        return;
                    }
                    lastSent = now;
                }
            }
            if (active.isCloseRequested()) {
                sink.closed();
            }
        } catch (ReadFailedException e) {
            fail(active, sink, e);
        } catch (RuntimeException e) {
            log.error(
                    "Consume session {} stopped unexpectedly", active.session().id(), e);
            fail(active, sink, new ReadFailedException("Reading stopped unexpectedly", e));
        } finally {
            release(active);
        }
    }

    /** @return false if the client left before receiving every message */
    private boolean deliverUndelivered(ActiveSession active, ConsumeSessionSink sink) {
        while (!active.undelivered().isEmpty()) {
            var message = JsonPayloads.markJson(active.undelivered().peek());
            if (!sink.message(message)) {
                return false;
            }
            active.undelivered().poll();
            active.session().recordActivity(clock.instant());
        }
        return true;
    }

    private void fail(ActiveSession active, ConsumeSessionSink sink, ReadFailedException failure) {
        registry.remove(active.session().id());
        active.session().close(clock.instant());
        active.requestClose();
        sink.failed(failure);
    }

    private void release(ActiveSession active) {
        if (active.release(clock.instant())) {
            active.stream().close();
        }
    }
}
