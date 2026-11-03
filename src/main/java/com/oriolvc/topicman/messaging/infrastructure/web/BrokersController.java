package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.application.ListBrokersCommand;
import com.oriolvc.topicman.messaging.application.ListBrokersUseCase;
import com.oriolvc.topicman.messaging.domain.BrokerCapabilities;
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

    private static BrokerResponse toResponse(BrokerCapabilities capabilities) {
        return new BrokerResponse(
                BrokerType.fromValue(capabilities.brokerType().name()),
                BrokerResponse.DestinationTypeEnum.fromValue(
                        capabilities.destinationType().name()),
                capabilities.canReadFromBeginning(),
                capabilities.supportsMessageKeys());
    }
}
