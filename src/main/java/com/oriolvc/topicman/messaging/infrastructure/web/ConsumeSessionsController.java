package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.application.CloseConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.CloseConsumeSessionUseCase;
import com.oriolvc.topicman.messaging.application.ConsumeSessionResult;
import com.oriolvc.topicman.messaging.application.OpenConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.OpenConsumeSessionUseCase;
import com.oriolvc.topicman.messaging.application.StreamConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.StreamConsumeSessionUseCase;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.messaging.infrastructure.web.api.ConsumeSessionsApi;
import com.oriolvc.topicman.messaging.infrastructure.web.model.ConsumeSessionResponse;
import com.oriolvc.topicman.messaging.infrastructure.web.model.OpenConsumeSessionRequest;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class ConsumeSessionsController implements ConsumeSessionsApi {

    // Sessions end when closed or idle, never because of the servlet container's async timeout.
    private static final long NO_TIMEOUT = 0L;

    private final OpenConsumeSessionUseCase openSession;
    private final StreamConsumeSessionUseCase streamSession;
    private final CloseConsumeSessionUseCase closeSession;

    ConsumeSessionsController(
            OpenConsumeSessionUseCase openSession,
            StreamConsumeSessionUseCase streamSession,
            CloseConsumeSessionUseCase closeSession) {
        this.openSession = openSession;
        this.streamSession = streamSession;
        this.closeSession = closeSession;
    }

    @Override
    public ResponseEntity<ConsumeSessionResponse> openConsumeSession(OpenConsumeSessionRequest request) {
        var startPosition = request.getStartPosition() == null
                ? StartPosition.LATEST
                : StartPosition.valueOf(request.getStartPosition().getValue());
        var result = openSession.execute(new OpenConsumeSessionCommand(
                new ConnectionId(request.getConnectionId()),
                WebMessages.toDestination(request.getDestination()),
                startPosition));
        var location = UriComponentsBuilder.fromPath(ConsumeSessionsApi.PATH_CLOSE_CONSUME_SESSION)
                .buildAndExpand(result.id().value())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(result));
    }

    @Override
    public ResponseEntity<SseEmitter> streamConsumeSessionEvents(UUID sessionId) {
        var emitter = new SseEmitter(NO_TIMEOUT);
        // Takes the stream over from a previous client, or throws before anything is streamed, so the
        // client gets a problem.
        streamSession.execute(
                new StreamConsumeSessionCommand(new ConsumeSessionId(sessionId), new SseConsumeSessionSink(emitter)));
        return ResponseEntity.ok(emitter);
    }

    @Override
    public ResponseEntity<Void> closeConsumeSession(UUID sessionId) {
        closeSession.execute(new CloseConsumeSessionCommand(new ConsumeSessionId(sessionId)));
        return ResponseEntity.noContent().build();
    }

    private static ConsumeSessionResponse toResponse(ConsumeSessionResult result) {
        var id = result.id().value();
        return new ConsumeSessionResponse(
                id,
                result.connectionId().value(),
                WebMessages.toDestinationDto(result.destination()),
                com.oriolvc.topicman.messaging.infrastructure.web.model.StartPosition.fromValue(
                        result.startPosition().name()),
                result.openedAt().atOffset(ZoneOffset.UTC),
                UriComponentsBuilder.fromPath(ConsumeSessionsApi.PATH_STREAM_CONSUME_SESSION_EVENTS)
                        .buildAndExpand(id)
                        .toUriString());
    }
}
