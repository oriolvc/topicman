package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.TlsSettings;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import org.jspecify.annotations.Nullable;

/** Changes the properties that are not null and keeps the others. */
public record UpdateConnectionCommand(
        ConnectionId id,
        @Nullable ConnectionName name,
        @Nullable BrokerAddress address,
        @Nullable Credentials credentials,
        @Nullable TlsSettings tls) {

    /** Keeps the TLS settings. */
    public UpdateConnectionCommand(
            ConnectionId id,
            @Nullable ConnectionName name,
            @Nullable BrokerAddress address,
            @Nullable Credentials credentials) {
        this(id, name, address, credentials, null);
    }
}
