package com.oriolvc.topicman.messaging.application;

import static com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenConsumeSessionUseCaseTest {

    private static final Destination ORDERS = Destination.topic("orders");

    private final InMemoryBrokerEndpoints endpoints = new InMemoryBrokerEndpoints();
    private final FakeMessageReader kafka = new FakeMessageReader(BrokerType.KAFKA);
    private final ConsumeSessionRegistry registry = new ConsumeSessionRegistry();
    private final OpenConsumeSessionUseCase useCase =
            new OpenConsumeSessionUseCase(endpoints, List.of(kafka), registry, new MutableClock(NOW));
    private final ConnectionId kafkaConnection = endpoints.add(BrokerType.KAFKA, "localhost:9092");

    @Test
    void opensASessionReadingTheDestination() {
        var result = useCase.execute(new OpenConsumeSessionCommand(kafkaConnection, ORDERS, StartPosition.EARLIEST));

        assertThat(result.connectionId()).isEqualTo(kafkaConnection);
        assertThat(result.destination()).isEqualTo(ORDERS);
        assertThat(result.startPosition()).isEqualTo(StartPosition.EARLIEST);
        assertThat(result.openedAt()).isEqualTo(NOW);
        assertThat(kafka.opened())
                .containsExactly(new FakeMessageReader.Opened(
                        new BrokerEndpoint(BrokerType.KAFKA, "localhost:9092"), ORDERS, StartPosition.EARLIEST));
        assertThat(registry.contains(result.id())).isTrue();
    }

    @Test
    void givesEverySessionItsOwnId() {
        var first = useCase.execute(new OpenConsumeSessionCommand(kafkaConnection, ORDERS, StartPosition.LATEST));
        var second = useCase.execute(new OpenConsumeSessionCommand(kafkaConnection, ORDERS, StartPosition.LATEST));

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void rejectsADestinationTheBrokerDoesNotSupport() {
        var exchange = Destination.exchange("orders", "");

        assertThatThrownBy(() ->
                        useCase.execute(new OpenConsumeSessionCommand(kafkaConnection, exchange, StartPosition.LATEST)))
                .isInstanceOf(InvalidDestinationException.class);
        assertThat(kafka.opened()).isEmpty();
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(
                        new OpenConsumeSessionCommand(ConnectionId.newId(), ORDERS, StartPosition.LATEST)))
                .isInstanceOf(ConnectionNotFoundException.class);
    }

    @Test
    void failsWhenTheDestinationCannotBeRead() {
        kafka.failToOpen(new ReadFailedException("Topic 'orders' does not exist"));

        assertThatThrownBy(() ->
                        useCase.execute(new OpenConsumeSessionCommand(kafkaConnection, ORDERS, StartPosition.LATEST)))
                .isInstanceOf(ReadFailedException.class);
    }

    @Test
    void failsForABrokerWithoutAReaderYet() {
        var pulsarConnection = endpoints.add(BrokerType.PULSAR, "pulsar://localhost:6650");

        assertThatThrownBy(() ->
                        useCase.execute(new OpenConsumeSessionCommand(pulsarConnection, ORDERS, StartPosition.LATEST)))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageContaining("PULSAR");
    }
}
