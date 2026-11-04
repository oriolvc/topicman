package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.TlsSettings;
import com.oriolvc.topicman.shared.domain.BrokerType;

public record CreateConnectionCommand(
        ConnectionName name, BrokerType brokerType, BrokerAddress address, Credentials credentials, TlsSettings tls) {

    /** A connection without TLS settings: no Kafka TLS, and the JVM's default CAs. */
    public CreateConnectionCommand(
            ConnectionName name, BrokerType brokerType, BrokerAddress address, Credentials credentials) {
        this(name, brokerType, address, credentials, TlsSettings.disabled());
    }
}
