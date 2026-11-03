package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.BrokerFeatures;
import com.oriolvc.topicman.collection.domain.DestinationKind;
import com.oriolvc.topicman.collection.domain.SupportedBrokers;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Kafka and Pulsar with topics and keys, RabbitMQ with exchanges and no keys; brokers can be removed. */
class FakeSupportedBrokers implements SupportedBrokers {

    private final Map<BrokerType, BrokerFeatures> features = new EnumMap<>(Map.of(
            BrokerType.KAFKA, new BrokerFeatures(BrokerType.KAFKA, DestinationKind.TOPIC, true),
            BrokerType.PULSAR, new BrokerFeatures(BrokerType.PULSAR, DestinationKind.TOPIC, true),
            BrokerType.RABBITMQ, new BrokerFeatures(BrokerType.RABBITMQ, DestinationKind.EXCHANGE, false)));

    void remove(BrokerType brokerType) {
        features.remove(brokerType);
    }

    @Override
    public Optional<BrokerFeatures> featuresOf(BrokerType brokerType) {
        return Optional.ofNullable(features.get(brokerType));
    }
}
