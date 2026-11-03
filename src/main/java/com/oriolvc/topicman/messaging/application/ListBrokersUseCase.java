package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerCapabilities;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Lists the brokers this server can both produce to and read from, with their capabilities. A
 * broker is supported once its adapter is registered, so adding one changes no code here.
 */
@Service
public class ListBrokersUseCase {

    private final ListBrokersResult result;

    public ListBrokersUseCase(List<MessageProducer> producers, List<MessageReader> readers) {
        Set<BrokerType> canProduce =
                producers.stream().map(MessageProducer::brokerType).collect(Collectors.toUnmodifiableSet());
        Set<BrokerType> canRead =
                readers.stream().map(MessageReader::brokerType).collect(Collectors.toUnmodifiableSet());
        // Adapters are fixed at startup, so the list is computed once.
        this.result = new ListBrokersResult(Arrays.stream(BrokerType.values())
                .filter(type -> canProduce.contains(type) && canRead.contains(type))
                .map(BrokerCapabilities::of)
                .toList());
    }

    public ListBrokersResult execute(ListBrokersCommand command) {
        return result;
    }
}
