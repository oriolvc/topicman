package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoints;
import com.oriolvc.topicman.messaging.domain.ConsumeSession;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OpenConsumeSessionUseCase {

    private final BrokerEndpoints endpoints;
    private final Map<BrokerType, MessageReader> readers;
    private final ConsumeSessionRegistry registry;
    private final Clock clock;

    public OpenConsumeSessionUseCase(
            BrokerEndpoints endpoints, List<MessageReader> readers, ConsumeSessionRegistry registry, Clock clock) {
        this.endpoints = endpoints;
        this.readers =
                readers.stream().collect(Collectors.toUnmodifiableMap(MessageReader::brokerType, Function.identity()));
        this.registry = registry;
        this.clock = clock;
    }

    /** Starts reading the destination; the messages are delivered when a client streams the session. */
    public ConsumeSessionResult execute(OpenConsumeSessionCommand command) {
        var endpoint = endpoints.of(command.connectionId());
        if (!command.destination().isSupportedBy(endpoint.type())) {
            throw new InvalidDestinationException(endpoint.type() + " does not support "
                    + command.destination().getClass().getSimpleName() + " destinations");
        }
        var reader = readers.get(endpoint.type());
        if (reader == null) {
            throw new ReadFailedException("Reading from " + endpoint.type() + " is not supported yet");
        }
        var session = ConsumeSession.open(
                command.connectionId(),
                endpoint.type(),
                command.destination(),
                command.startPosition(),
                clock.instant());
        var stream = reader.open(endpoint, command.destination(), command.startPosition());
        registry.register(new ActiveSession(session, stream));
        return ConsumeSessionResult.of(session);
    }
}
