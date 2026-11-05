package com.oriolvc.topicman.messaging.application.usecase;

import com.oriolvc.topicman.messaging.application.helper.ActiveSession;
import com.oriolvc.topicman.messaging.application.helper.ConsumeSessionRegistry;
import com.oriolvc.topicman.messaging.application.model.ConsumeSessionLimits;
import com.oriolvc.topicman.messaging.application.model.ConsumeSessionResult;
import com.oriolvc.topicman.messaging.application.model.OpenConsumeSessionCommand;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionLimitReachedException;
import com.oriolvc.topicman.messaging.domain.exception.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.ConsumeSession;
import com.oriolvc.topicman.messaging.domain.port.BrokerEndpoints;
import com.oriolvc.topicman.messaging.domain.port.MessageReader;
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
    private final ConsumeSessionLimits limits;
    private final Clock clock;

    public OpenConsumeSessionUseCase(
            BrokerEndpoints endpoints,
            List<MessageReader> readers,
            ConsumeSessionRegistry registry,
            ConsumeSessionLimits limits,
            Clock clock) {
        this.endpoints = endpoints;
        this.readers =
                readers.stream().collect(Collectors.toUnmodifiableMap(MessageReader::brokerType, Function.identity()));
        this.registry = registry;
        this.limits = limits;
        this.clock = clock;
    }

    /** Starts reading the destination; the messages are delivered when a client streams the session. */
    public ConsumeSessionResult execute(OpenConsumeSessionCommand command) {
        // Checked first so a full registry never opens a broker client, and again on registering
        // because sessions may have been opened meanwhile.
        if (registry.size() >= limits.maxOpen()) {
            throw new ConsumeSessionLimitReachedException(limits.maxOpen());
        }
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
        if (!registry.registerWithin(new ActiveSession(session, stream), limits.maxOpen())) {
            stream.close();
            throw new ConsumeSessionLimitReachedException(limits.maxOpen());
        }
        return ConsumeSessionResult.of(session);
    }
}
