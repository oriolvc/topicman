package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ConsumeSessionInUseException;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionNotFoundException;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class StreamConsumeSessionUseCase {

    static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(15);
    // Well above the poll timeout, after which a streaming thread notices it was replaced.
    static final Duration HANDOVER_TIMEOUT = Duration.ofSeconds(5);

    private static final Logger log = LoggerFactory.getLogger(StreamConsumeSessionUseCase.class);

    private final ConsumeSessionRegistry registry;
    private final Executor executor;
    private final Clock clock;
    private final Duration handoverTimeout;

    @Autowired
    public StreamConsumeSessionUseCase(
            ConsumeSessionRegistry registry, @Qualifier("consumeSessionExecutor") Executor executor, Clock clock) {
        this(registry, executor, clock, HANDOVER_TIMEOUT);
    }

    StreamConsumeSessionUseCase(
            ConsumeSessionRegistry registry, Executor executor, Clock clock, Duration handoverTimeout) {
        this.registry = registry;
        this.executor = executor;
        this.clock = clock;
        this.handoverTimeout = handoverTimeout;
    }

    /**
     * Claims the session for this client, taking it over from the client streaming it, if any. Fails
     * if the session is unknown or closes meanwhile, or the stream is not handed over in time. Then
     * streams it on the executor until the client leaves, another client takes over or the session
     * closes. Reading pauses while no client is connected, so the next client continues where this
     * one stopped.
     */
    public void execute(StreamConsumeSessionCommand command) {
        var active = registry.get(command.id());
        var claim = active.claim(handoverTimeout, clock.instant())
                .orElseThrow(() -> active.isCloseRequested()
                        ? new ConsumeSessionNotFoundException(command.id())
                        : new ConsumeSessionInUseException(command.id()));
        try {
            executor.execute(() -> stream(active, claim, command.sink()));
        } catch (RuntimeException e) {
            release(active);
            throw e;
        }
    }

    private void stream(ActiveSession active, long claim, ConsumeSessionSink sink) {
        try {
            var lastSent = clock.instant();
            while (!active.isCloseRequested() && active.isClaimedBy(claim) && sink.isConnected()) {
                if (!deliverUndelivered(active, claim, sink)) {
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
            } else if (!active.isClaimedBy(claim)) {
                sink.replaced();
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

    /** Stops early if another client takes over. @return false if the client left meanwhile */
    private boolean deliverUndelivered(ActiveSession active, long claim, ConsumeSessionSink sink) {
        while (!active.undelivered().isEmpty() && active.isClaimedBy(claim)) {
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
