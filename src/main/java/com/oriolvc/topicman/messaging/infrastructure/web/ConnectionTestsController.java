package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.application.TestConnectionCommand;
import com.oriolvc.topicman.messaging.application.TestConnectionUseCase;
import com.oriolvc.topicman.messaging.infrastructure.web.api.ConnectionTestsApi;
import com.oriolvc.topicman.messaging.infrastructure.web.model.ConnectionTestResponse;
import com.oriolvc.topicman.messaging.infrastructure.web.model.TestConnectionRequest;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ConnectionTestsController implements ConnectionTestsApi {

    private final TestConnectionUseCase testConnection;

    ConnectionTestsController(TestConnectionUseCase testConnection) {
        this.testConnection = testConnection;
    }

    @Override
    public ResponseEntity<ConnectionTestResponse> testConnection(TestConnectionRequest request) {
        var result = testConnection.execute(new TestConnectionCommand(new ConnectionId(request.getConnectionId())));
        var status = result.outcome().succeeded()
                ? ConnectionTestResponse.StatusEnum.SUCCEEDED
                : ConnectionTestResponse.StatusEnum.FAILED;
        return ResponseEntity.ok(new ConnectionTestResponse(
                status, result.outcome().detail(), result.duration().toMillis()));
    }
}
