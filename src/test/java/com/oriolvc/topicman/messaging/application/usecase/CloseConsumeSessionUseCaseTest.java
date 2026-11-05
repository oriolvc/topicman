package com.oriolvc.topicman.messaging.application.usecase;

import static com.oriolvc.topicman.messaging.application.usecase.ConsumeSessionFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.application.helper.ConsumeSessionRegistry;
import com.oriolvc.topicman.messaging.application.model.CloseConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.model.OpenConsumeSessionCommand;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionNotFoundException;
import com.oriolvc.topicman.messaging.domain.model.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.List;
import org.junit.jupiter.api.Test;

class CloseConsumeSessionUseCaseTest {

    private final InMemoryBrokerEndpoints endpoints = new InMemoryBrokerEndpoints();
    private final FakeMessageReader kafka = new FakeMessageReader(BrokerType.KAFKA);
    private final ConsumeSessionRegistry registry = new ConsumeSessionRegistry();
    private final MutableClock clock = new MutableClock(NOW);
    private final OpenConsumeSessionUseCase open =
            new OpenConsumeSessionUseCase(endpoints, List.of(kafka), registry, ConsumeSessionFixtures.LIMITS, clock);
    private final CloseConsumeSessionUseCase useCase = new CloseConsumeSessionUseCase(registry, clock);

    @Test
    void closesTheSessionAndReleasesTheBrokerStream() {
        var session = openSession();

        useCase.execute(new CloseConsumeSessionCommand(session));

        assertThat(registry.contains(session)).isFalse();
        assertThat(kafka.lastStream().isClosed()).isTrue();
    }

    @Test
    void cannotCloseASessionTwice() {
        var session = openSession();
        useCase.execute(new CloseConsumeSessionCommand(session));

        assertThatThrownBy(() -> useCase.execute(new CloseConsumeSessionCommand(session)))
                .isInstanceOf(ConsumeSessionNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "messaging.session-not-found");
    }

    @Test
    void failsForAnUnknownSession() {
        assertThatThrownBy(() -> useCase.execute(new CloseConsumeSessionCommand(ConsumeSessionId.newId())))
                .isInstanceOf(ConsumeSessionNotFoundException.class);
    }

    private ConsumeSessionId openSession() {
        var connection = endpoints.add(BrokerType.KAFKA, "localhost:9092");
        return open.execute(
                        new OpenConsumeSessionCommand(connection, Destination.topic("orders"), StartPosition.LATEST))
                .id();
    }
}
