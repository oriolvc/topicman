package com.oriolvc.topicman.connection.application.usecase;

import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

final class ConnectionFixtures {

    static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private ConnectionFixtures() {}

    static CreateConnectionCommand aCreateKafkaConnectionCommand(String name) {
        return new CreateConnectionCommand(
                new ConnectionName(name), BrokerType.KAFKA, new BrokerAddress("localhost:9092"), Credentials.none());
    }
}
