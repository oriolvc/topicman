package com.oriolvc.topicman.collection.infrastructure.messaging;

import com.oriolvc.topicman.collection.domain.model.BrokerFeatures;
import com.oriolvc.topicman.collection.domain.model.DestinationKind;
import com.oriolvc.topicman.collection.domain.port.SupportedBrokers;
import com.oriolvc.topicman.messaging.application.model.ListBrokersCommand;
import com.oriolvc.topicman.messaging.application.model.SupportedBroker;
import com.oriolvc.topicman.messaging.application.usecase.ListBrokersUseCase;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Asks the messaging context which brokers it supports, through its application API (ADR 0006). */
@Component
class MessagingSupportedBrokers implements SupportedBrokers {

    private final ListBrokersUseCase listBrokers;

    MessagingSupportedBrokers(ListBrokersUseCase listBrokers) {
        this.listBrokers = listBrokers;
    }

    @Override
    public Optional<BrokerFeatures> featuresOf(BrokerType brokerType) {
        return listBrokers.execute(new ListBrokersCommand()).brokers().stream()
                .filter(broker -> broker.type() == brokerType)
                .findFirst()
                .map(MessagingSupportedBrokers::toFeatures);
    }

    private static BrokerFeatures toFeatures(SupportedBroker broker) {
        return new BrokerFeatures(
                broker.type(), DestinationKind.valueOf(broker.destinationKind().name()), broker.supportsMessageKeys());
    }
}
