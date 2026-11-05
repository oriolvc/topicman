package com.oriolvc.topicman.collection.domain.port;

import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.model.BrokerFeatures;
import com.oriolvc.topicman.collection.domain.model.MessageTemplate;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Optional;

/** Outbound port: the brokers templates can target, as the messaging context supports them. */
public interface SupportedBrokers {

    /** What the broker accepts, or empty if this server cannot produce to it. */
    Optional<BrokerFeatures> featuresOf(BrokerType brokerType);

    /**
     * @throws InvalidTemplateException if this server cannot produce to the template's broker, or
     *     the template does not fit it
     */
    default void check(MessageTemplate template) {
        featuresOf(template.brokerType())
                .orElseThrow(() ->
                        new InvalidTemplateException("Templates for " + template.brokerType() + " are not supported"))
                .check(template);
    }
}
