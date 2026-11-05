package com.oriolvc.topicman.messaging.application.model;

import java.util.List;

/** @param brokers the supported brokers, in {@link com.oriolvc.topicman.shared.domain.BrokerType} order */
public record ListBrokersResult(List<SupportedBroker> brokers) {

    public ListBrokersResult {
        brokers = List.copyOf(brokers);
    }
}
