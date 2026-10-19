package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.application.ConsumeSessionSink;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import com.oriolvc.topicman.messaging.infrastructure.web.model.ConsumedMessageEvent;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Streams a consume session as server-sent events: {@code message}, {@code failed}, {@code closed}
 * and {@code replaced} events, with a comment as heartbeat (see messaging.yaml).
 */
final class SseConsumeSessionSink implements ConsumeSessionSink {

    private final SseEmitter emitter;
    private final AtomicBoolean connected = new AtomicBoolean(true);

    SseConsumeSessionSink(SseEmitter emitter) {
        this.emitter = emitter;
        emitter.onCompletion(() -> connected.set(false));
        emitter.onTimeout(() -> connected.set(false));
        emitter.onError(error -> connected.set(false));
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public boolean message(ReceivedMessage received) {
        var message = received.message();
        var event = new ConsumedMessageEvent(
                        message.headers().values(),
                        WebMessages.toPayload(message.payload()),
                        received.timestamp().atOffset(ZoneOffset.UTC),
                        received.position())
                .key(message.key());
        return send(SseEmitter.event().name("message").id(received.position()).data(event, MediaType.APPLICATION_JSON));
    }

    @Override
    public boolean heartbeat() {
        return send(SseEmitter.event().comment("heartbeat"));
    }

    @Override
    public void failed(ReadFailedException failure) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, failure.getMessage());
        problem.setTitle("Read failed");
        problem.setProperty("code", failure.code());
        send(SseEmitter.event().name("failed").data(problem, MediaType.APPLICATION_JSON));
        emitter.complete();
    }

    @Override
    public void closed() {
        end("closed");
    }

    @Override
    public void replaced() {
        end("replaced");
    }

    private void end(String event) {
        send(SseEmitter.event().name(event).data(""));
        emitter.complete();
    }

    private boolean send(SseEmitter.SseEventBuilder event) {
        if (!connected.get()) {
            return false;
        }
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            // The client went away; the session stays open until it reconnects or is closed.
            connected.set(false);
            return false;
        }
    }
}
