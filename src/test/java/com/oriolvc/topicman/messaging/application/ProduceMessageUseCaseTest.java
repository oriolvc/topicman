package com.oriolvc.topicman.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProduceMessageUseCaseTest {

    private static final Destination ORDERS = Destination.topic("orders");

    private final InMemoryBrokerEndpoints endpoints = new InMemoryBrokerEndpoints();
    private final FakeMessageProducer kafka = new FakeMessageProducer(BrokerType.KAFKA);
    private final FakeMessageProducer rabbitMq = new FakeMessageProducer(BrokerType.RABBITMQ);
    private final ProduceMessageUseCase useCase = new ProduceMessageUseCase(endpoints, List.of(kafka, rabbitMq));
    private final ConnectionId kafkaConnection = endpoints.add(BrokerType.KAFKA, "localhost:9092");

    @Test
    void producesTheMessageToTheConnectionsBroker() {
        var message = Message.of(Payload.json("{\"id\":42}")).withKey("order-42");

        var result = useCase.execute(new ProduceMessageCommand(kafkaConnection, ORDERS, message, 1));

        assertThat(result.receipts()).containsExactly(new SendReceipt("receipt-1"));
        assertThat(kafka.sent())
                .containsExactly(new FakeMessageProducer.Sent(
                        new BrokerEndpoint(BrokerType.KAFKA, "localhost:9092"), ORDERS, message));
    }

    @Test
    void producesEveryCopyInOrder() {
        var result =
                useCase.execute(new ProduceMessageCommand(kafkaConnection, ORDERS, Message.of(Payload.text("hi")), 3));

        assertThat(result.receipts())
                .containsExactly(
                        new SendReceipt("receipt-1"), new SendReceipt("receipt-2"), new SendReceipt("receipt-3"));
    }

    @Test
    void rejectsAJsonPayloadThatIsNotWellFormed() {
        var message = Message.of(Payload.json("{\"id\": 42"));

        assertThatThrownBy(() -> useCase.execute(new ProduceMessageCommand(kafkaConnection, ORDERS, message, 1)))
                .isInstanceOf(InvalidMessageException.class);
        assertThat(kafka.sent()).isEmpty();
    }

    @Test
    void rejectsAJsonPayloadWithTrailingContent() {
        var message = Message.of(Payload.json("{\"id\": 42} trailing"));

        assertThatThrownBy(() -> useCase.execute(new ProduceMessageCommand(kafkaConnection, ORDERS, message, 1)))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void acceptsATextPayloadThatIsNotJson() {
        var message = Message.of(Payload.text("{not json"));

        var result = useCase.execute(new ProduceMessageCommand(kafkaConnection, ORDERS, message, 1));

        assertThat(result.receipts()).hasSize(1);
    }

    @Test
    void rejectsADestinationTheBrokerDoesNotSupport() {
        var exchange = Destination.exchange("orders", "created");

        assertThatThrownBy(() -> useCase.execute(
                        new ProduceMessageCommand(kafkaConnection, exchange, Message.of(Payload.text("hi")), 1)))
                .isInstanceOf(InvalidDestinationException.class);
        assertThat(kafka.sent()).isEmpty();
    }

    @Test
    void rejectsAKeyForABrokerWithoutMessageKeys() {
        var rabbitMqConnection = endpoints.add(BrokerType.RABBITMQ, "amqp://localhost:5672");
        var message = Message.of(Payload.text("hi")).withKey("order-42");
        var exchange = Destination.exchange("orders", "created");

        assertThatThrownBy(() -> useCase.execute(new ProduceMessageCommand(rabbitMqConnection, exchange, message, 1)))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("RABBITMQ");
        assertThat(rabbitMq.sent()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsACopyCountOutOfRange(int copies) {
        assertThatThrownBy(() -> useCase.execute(
                        new ProduceMessageCommand(kafkaConnection, ORDERS, Message.of(Payload.text("hi")), copies)))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(
                        new ProduceMessageCommand(TestConnectionIds.next(), ORDERS, Message.of(Payload.text("hi")), 1)))
                .isInstanceOf(ConnectionNotFoundException.class);
    }

    @Test
    void tellsHowManyCopiesWereSentBeforeAFailure() {
        kafka.failAfter(2);

        assertThatThrownBy(() -> useCase.execute(
                        new ProduceMessageCommand(kafkaConnection, ORDERS, Message.of(Payload.text("hi")), 5)))
                .isInstanceOfSatisfying(
                        ProduceFailedException.class,
                        failure -> assertThat(failure.sent()).isEqualTo(2));
        assertThat(kafka.sent()).hasSize(2);
    }

    @Test
    void failsForABrokerWithoutAProducerYet() {
        var pulsarConnection = endpoints.add(BrokerType.PULSAR, "pulsar://localhost:6650");

        assertThatThrownBy(() -> useCase.execute(
                        new ProduceMessageCommand(pulsarConnection, ORDERS, Message.of(Payload.text("hi")), 1)))
                .isInstanceOf(ProduceFailedException.class)
                .hasMessageContaining("PULSAR");
    }
}
