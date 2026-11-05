package com.oriolvc.topicman.messaging.application.usecase;

import com.oriolvc.topicman.messaging.application.model.TestConnectionCommand;
import com.oriolvc.topicman.messaging.application.model.TestConnectionResult;
import com.oriolvc.topicman.messaging.domain.model.ConnectionTestOutcome;
import com.oriolvc.topicman.messaging.domain.port.BrokerEndpoints;
import com.oriolvc.topicman.messaging.domain.port.ConnectionTester;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Checks that a saved connection reaches its broker; an unreachable broker is a failed outcome. */
@Service
public class TestConnectionUseCase {

    private final BrokerEndpoints endpoints;
    private final Map<BrokerType, ConnectionTester> testers;
    private final Clock clock;

    public TestConnectionUseCase(BrokerEndpoints endpoints, List<ConnectionTester> testers, Clock clock) {
        this.endpoints = endpoints;
        this.testers = testers.stream()
                .collect(Collectors.toUnmodifiableMap(ConnectionTester::brokerType, Function.identity()));
        this.clock = clock;
    }

    public TestConnectionResult execute(TestConnectionCommand command) {
        var endpoint = endpoints.of(command.connectionId());
        var tester = testers.get(endpoint.type());
        var start = clock.instant();
        var outcome = tester == null
                ? ConnectionTestOutcome.failed("Testing " + endpoint.type() + " connections is not supported yet")
                : tester.test(endpoint);
        return new TestConnectionResult(outcome, Duration.between(start, clock.instant()));
    }
}
