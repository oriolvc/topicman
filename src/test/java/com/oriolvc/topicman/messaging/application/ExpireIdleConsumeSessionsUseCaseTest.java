package com.oriolvc.topicman.messaging.application;

import static com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.LIMITS;
import static com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.NOW;
import static com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.aReceivedText;
import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.CapturingExecutor;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ExpireIdleConsumeSessionsUseCaseTest {

    private static final Duration IDLE_TIMEOUT = LIMITS.idleTimeout();

    private final InMemoryBrokerEndpoints endpoints = new InMemoryBrokerEndpoints();
    private final FakeMessageReader kafka = new FakeMessageReader(BrokerType.KAFKA);
    private final ConsumeSessionRegistry registry = new ConsumeSessionRegistry();
    private final MutableClock clock = new MutableClock(NOW);
    private final OpenConsumeSessionUseCase open =
            new OpenConsumeSessionUseCase(endpoints, List.of(kafka), registry, LIMITS, clock);
    private final ExpireIdleConsumeSessionsUseCase useCase =
            new ExpireIdleConsumeSessionsUseCase(registry, LIMITS, clock);

    @Test
    void closesASessionNoClientStreamedForTheIdleTimeout() {
        var session = openSession();
        clock.set(NOW.plus(IDLE_TIMEOUT));

        var result = useCase.execute(new ExpireIdleConsumeSessionsCommand());

        assertThat(result.expired()).isEqualTo(1);
        assertThat(registry.contains(session)).isFalse();
        assertThat(kafka.lastStream().isClosed()).isTrue();
    }

    @Test
    void keepsASessionBeforeTheIdleTimeout() {
        var session = openSession();
        clock.set(NOW.plus(IDLE_TIMEOUT).minusSeconds(1));

        var result = useCase.execute(new ExpireIdleConsumeSessionsCommand());

        assertThat(result.expired()).isZero();
        assertThat(registry.contains(session)).isTrue();
    }

    @Test
    void neverExpiresASessionWhileAClientStreamsIt() {
        var session = openSession();
        new StreamConsumeSessionUseCase(registry, new CapturingExecutor(), clock)
                .execute(new StreamConsumeSessionCommand(session, new RecordingSink()));
        clock.set(NOW.plus(IDLE_TIMEOUT.multipliedBy(10)));

        useCase.execute(new ExpireIdleConsumeSessionsCommand());

        assertThat(registry.contains(session)).isTrue();
        assertThat(kafka.lastStream().isClosed()).isFalse();
    }

    @Test
    void countsIdleTimeFromWhenTheLastClientLeft() {
        var session = openSession();
        kafka.lastStream().willReturn(List.of(aReceivedText("hello", "p@0")));
        var clientLeftAt = NOW.plus(IDLE_TIMEOUT.multipliedBy(2));
        clock.set(clientLeftAt);
        new StreamConsumeSessionUseCase(registry, Runnable::run, clock)
                .execute(new StreamConsumeSessionCommand(session, new RecordingSink().leavingAfterMessages(1)));
        clock.set(clientLeftAt.plus(IDLE_TIMEOUT).minusSeconds(1));

        useCase.execute(new ExpireIdleConsumeSessionsCommand());

        assertThat(registry.contains(session)).isTrue();
    }

    private ConsumeSessionId openSession() {
        var connection = endpoints.add(BrokerType.KAFKA, "localhost:9092");
        return open.execute(
                        new OpenConsumeSessionCommand(connection, Destination.topic("orders"), StartPosition.LATEST))
                .id();
    }
}
