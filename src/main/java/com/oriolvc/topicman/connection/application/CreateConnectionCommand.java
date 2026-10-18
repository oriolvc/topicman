package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.shared.domain.BrokerType;

public record CreateConnectionCommand(
        ConnectionName name, BrokerType brokerType, BrokerAddress address, Credentials credentials) {}
