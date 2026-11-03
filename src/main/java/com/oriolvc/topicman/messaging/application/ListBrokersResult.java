package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerCapabilities;
import java.util.List;

/** @param brokers the supported brokers, in {@link com.oriolvc.topicman.shared.domain.BrokerType} order */
public record ListBrokersResult(List<BrokerCapabilities> brokers) {

    public ListBrokersResult {
        brokers = List.copyOf(brokers);
    }
}
