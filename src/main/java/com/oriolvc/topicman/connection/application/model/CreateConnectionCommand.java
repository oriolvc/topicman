package com.oriolvc.topicman.connection.application.model;

import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TlsSettings;
import com.oriolvc.topicman.shared.domain.BrokerType;

public record CreateConnectionCommand(
        ConnectionName name, BrokerType brokerType, BrokerAddress address, Credentials credentials, TlsSettings tls) {

    /** A connection without TLS settings: no Kafka TLS, and the JVM's default CAs. */
    public CreateConnectionCommand(
            ConnectionName name, BrokerType brokerType, BrokerAddress address, Credentials credentials) {
        this(name, brokerType, address, credentials, TlsSettings.disabled());
    }
}
