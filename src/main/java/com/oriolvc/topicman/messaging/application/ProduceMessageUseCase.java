package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoints;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProduceMessageUseCase {

    static final int MAX_COPIES = 100;

    private final BrokerEndpoints endpoints;
    private final Map<BrokerType, MessageProducer> producers;

    public ProduceMessageUseCase(BrokerEndpoints endpoints, List<MessageProducer> producers) {
        this.endpoints = endpoints;
        this.producers = producers.stream()
                .collect(Collectors.toUnmodifiableMap(MessageProducer::brokerType, Function.identity()));
    }

    /** Sends the copies one after the other, stopping at the first failure. */
    public ProduceMessageResult execute(ProduceMessageCommand command) {
        if (command.copies() < 1 || command.copies() > MAX_COPIES) {
            throw new InvalidMessageException("Copies must be between 1 and " + MAX_COPIES);
        }
        JsonPayloads.requireWellFormed(command.message().payload());
        var endpoint = endpoints.of(command.connectionId());
        if (!command.destination().isSupportedBy(endpoint.type())) {
            throw new InvalidDestinationException(endpoint.type() + " does not support "
                    + command.destination().getClass().getSimpleName() + " destinations");
        }
        var producer = producers.get(endpoint.type());
        if (producer == null) {
            throw new ProduceFailedException("Producing to " + endpoint.type() + " is not supported yet");
        }
        var receipts = new ArrayList<SendReceipt>();
        for (int copy = 0; copy < command.copies(); copy++) {
            try {
                receipts.add(producer.send(endpoint, command.destination(), command.message()));
            } catch (ProduceFailedException e) {
                throw ProduceFailedException.afterSending(receipts.size(), e);
            }
        }
        return new ProduceMessageResult(receipts);
    }
}
