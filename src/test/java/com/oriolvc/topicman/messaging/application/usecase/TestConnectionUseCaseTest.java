package com.oriolvc.topicman.messaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.application.model.TestConnectionCommand;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.ConnectionTestOutcome;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestConnectionUseCaseTest {

    private final InMemoryBrokerEndpoints endpoints = new InMemoryBrokerEndpoints();
    private final FakeConnectionTester kafka = new FakeConnectionTester(BrokerType.KAFKA);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-03T10:00:00Z"));
    private final TestConnectionUseCase useCase = new TestConnectionUseCase(endpoints, List.of(kafka), clock);

    @Test
    void testsTheConnectionsEndpointAndTimesIt() {
        var connection = endpoints.add(BrokerType.KAFKA, "localhost:9092");
        kafka.answer(ConnectionTestOutcome.succeeded("Kafka cluster abc, 1 broker"));
        clock.advanceOnEveryRead(Duration.ofMillis(120));

        var result = useCase.execute(new TestConnectionCommand(connection));

        assertThat(result.outcome()).isEqualTo(ConnectionTestOutcome.succeeded("Kafka cluster abc, 1 broker"));
        assertThat(result.duration()).isEqualTo(Duration.ofMillis(120));
        assertThat(kafka.tested()).containsExactly(new BrokerEndpoint(BrokerType.KAFKA, "localhost:9092"));
    }

    @Test
    void reportsAFailedTestAsItsOutcome() {
        var connection = endpoints.add(BrokerType.KAFKA, "localhost:1");
        kafka.answer(ConnectionTestOutcome.failed("Connection refused"));

        var result = useCase.execute(new TestConnectionCommand(connection));

        assertThat(result.outcome()).isEqualTo(ConnectionTestOutcome.failed("Connection refused"));
    }

    @Test
    void failsTheTestOfABrokerWithoutATester() {
        var connection = endpoints.add(BrokerType.PULSAR, "pulsar://localhost:6650");

        var result = useCase.execute(new TestConnectionCommand(connection));

        assertThat(result.outcome().succeeded()).isFalse();
        assertThat(result.outcome().detail()).contains("PULSAR");
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(new TestConnectionCommand(TestConnectionIds.next())))
                .isInstanceOf(ConnectionNotFoundException.class);
    }
}
