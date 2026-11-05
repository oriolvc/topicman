package com.oriolvc.topicman.connection.application.model;

import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.connection.domain.model.TlsSettings;
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
