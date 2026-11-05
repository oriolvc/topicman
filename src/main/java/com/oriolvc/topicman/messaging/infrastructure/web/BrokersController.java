package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.application.model.ListBrokersCommand;
import com.oriolvc.topicman.messaging.application.model.SupportedBroker;
import com.oriolvc.topicman.messaging.application.usecase.ListBrokersUseCase;
import com.oriolvc.topicman.messaging.infrastructure.web.api.BrokersApi;
import com.oriolvc.topicman.messaging.infrastructure.web.model.BrokerResponse;
import com.oriolvc.topicman.messaging.infrastructure.web.model.BrokerType;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BrokersController implements BrokersApi {

    private final ListBrokersUseCase listBrokers;

    BrokersController(ListBrokersUseCase listBrokers) {
        this.listBrokers = listBrokers;
    }

    @Override
    public ResponseEntity<List<BrokerResponse>> listBrokers() {
        var result = listBrokers.execute(new ListBrokersCommand());
        return ResponseEntity.ok(
                result.brokers().stream().map(BrokersController::toResponse).toList());
    }

    private static BrokerResponse toResponse(SupportedBroker broker) {
        return new BrokerResponse(
                BrokerType.fromValue(broker.type().name()),
                BrokerResponse.DestinationTypeEnum.fromValue(
                        broker.destinationKind().name()),
                broker.canReadFromBeginning(),
                broker.supportsMessageKeys());
    }
}
