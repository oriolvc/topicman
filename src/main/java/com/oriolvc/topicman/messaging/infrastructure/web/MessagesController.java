package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.application.ProduceMessageCommand;
import com.oriolvc.topicman.messaging.application.ProduceMessageUseCase;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.messaging.infrastructure.web.api.MessagesApi;
import com.oriolvc.topicman.messaging.infrastructure.web.model.ProduceMessageRequest;
import com.oriolvc.topicman.messaging.infrastructure.web.model.ProduceMessageResponse;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MessagesController implements MessagesApi {

    private final ProduceMessageUseCase produceMessage;

    MessagesController(ProduceMessageUseCase produceMessage) {
        this.produceMessage = produceMessage;
    }

    @Override
    public ResponseEntity<ProduceMessageResponse> produceMessage(ProduceMessageRequest request) {
        var result = produceMessage.execute(new ProduceMessageCommand(
                new ConnectionId(request.getConnectionId()),
                WebMessages.toDestination(request.getDestination()),
                WebMessages.toMessage(request.getKey(), request.getHeaders(), request.getPayload()),
                request.getCopies()));
        return ResponseEntity.ok(new ProduceMessageResponse(
                result.receipts().stream().map(SendReceipt::reference).toList()));
    }
}
