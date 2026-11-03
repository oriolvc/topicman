package com.oriolvc.topicman.messaging.infrastructure.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.messaging.domain.BrokerCapabilities;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.ConnectionTester;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The behaviour every broker adapter must have, checked only through the {@link MessageProducer}
 * and {@link MessageReader} ports. Each adapter's integration test extends it with a real broker.
 * Where brokers legitimately differ, the difference is an explicit capability below, never a
 * skipped test.
 */
public abstract class BrokerAdapterContractTest {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    // Well below any adapter's send timeout, so waiting it out cannot pass.
    private static final Duration UNREACHABLE_TIMEOUT = Duration.ofSeconds(1);

    // AMQP timestamps have a resolution of one second.
    private static final Duration CLOCK_TOLERANCE = Duration.ofSeconds(1);

    protected MessageProducer producer;
    protected MessageReader reader;
    protected ConnectionTester tester;

    protected abstract BrokerType brokerType();

    protected abstract MessageProducer createProducer();

    protected abstract MessageReader createReader();

    protected abstract ConnectionTester createTester();

    protected abstract BrokerEndpoint endpoint();

    /** An endpoint where no broker listens. */
    protected abstract BrokerEndpoint unreachableEndpoint();

    /** A new, empty destination that exists on the broker, with ordered delivery. */
    protected abstract Destination newDestination();

    /** A well-formed destination that does not exist on the broker. */
    protected abstract Destination missingDestination();

    /** A destination of a kind this broker does not support. */
    protected abstract Destination unsupportedDestination();

    /**
     * What the API tells clients about this broker. The contract checks the adapter against it, so
     * the two cannot drift apart.
     */
    protected final BrokerCapabilities capabilities() {
        return BrokerCapabilities.of(brokerType());
    }

    /** Asserts that reading left no consumer state behind, such as a group, subscription or queue. */
    protected abstract void assertNoReaderStateLeftFor(Destination destination);

    @BeforeEach
    void createAdapters() {
        producer = createProducer();
        reader = createReader();
        tester = createTester();
    }

    @AfterEach
    void closeAdapters() throws Exception {
        if (producer instanceof AutoCloseable closeable) {
            closeable.close();
        }
        if (reader instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    @Test
    void reportsItsBrokerType() {
        assertThat(producer.brokerType()).isEqualTo(brokerType());
        assertThat(reader.brokerType()).isEqualTo(brokerType());
        assertThat(tester.brokerType()).isEqualTo(brokerType());
    }

    @Test
    void readsAProducedMessageWithItsKeyHeadersAndPayload() {
        var destination = newDestination();
        var message = Message.of(Payload.json("{\"id\":42}"))
                .withHeaders(Headers.of(Map.of("source", "topicman", "trace-id", "abc-123")));
        if (capabilities().supportsMessageKeys()) {
            message = message.withKey("order-42");
        }

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            producer.send(endpoint(), destination, message);

            var received = pollUntil(stream, 1).getFirst().message();
            assertThat(received.key()).isEqualTo(message.key());
            assertThat(received.headers()).isEqualTo(message.headers());
            assertThat(received.payload().content()).isEqualTo("{\"id\":42}");
        }
    }

    @Test
    void acknowledgesAndPositionsEveryMessage() {
        var destination = newDestination();

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            var before = Instant.now();
            var receipt = producer.send(endpoint(), destination, Message.of(Payload.text("hello")));
            var received = pollUntil(stream, 1).getFirst();
            var after = Instant.now();

            assertThat(receipt.reference()).isNotBlank();
            assertThat(received.position()).isNotBlank();
            assertThat(received.timestamp()).isBetween(before.minus(CLOCK_TOLERANCE), after.plus(CLOCK_TOLERANCE));
        }
    }

    @Test
    void readsMessagesInTheOrderTheyWereProduced() {
        var destination = newDestination();
        var payloads = IntStream.rangeClosed(1, 5).mapToObj(i -> "message-" + i).toList();

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            payloads.forEach(payload -> producer.send(endpoint(), destination, Message.of(Payload.text(payload))));

            assertThat(payloadsOf(pollUntil(stream, 5))).isEqualTo(payloads);
        }
    }

    @Test
    void latestReadsOnlyMessagesProducedAfterOpening() {
        var destination = newDestination();
        producer.send(endpoint(), destination, Message.of(Payload.text("before")));

        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            producer.send(endpoint(), destination, Message.of(Payload.text("after")));

            assertThat(payloadsOf(pollUntil(stream, 1))).containsExactly("after");
        }
    }

    @Test
    void earliestReadsRetainedMessagesOnlyWhenTheBrokerCan() {
        var destination = newDestination();
        producer.send(endpoint(), destination, Message.of(Payload.text("first")));
        producer.send(endpoint(), destination, Message.of(Payload.text("second")));

        try (var stream = reader.open(endpoint(), destination, StartPosition.EARLIEST)) {
            producer.send(endpoint(), destination, Message.of(Payload.text("third")));

            // If EARLIEST cannot show retained messages, it must behave like LATEST (ADR 0003).
            var expected =
                    capabilities().canReadFromBeginning() ? List.of("first", "second", "third") : List.of("third");
            assertThat(payloadsOf(pollUntil(stream, expected.size()))).isEqualTo(expected);
        }
    }

    @Test
    void returnsNothingWhenNoMessageArrives() {
        try (var stream = reader.open(endpoint(), newDestination(), StartPosition.EARLIEST)) {
            assertThat(stream.poll(Duration.ofMillis(500))).isEmpty();
        }
    }

    @Test
    void readingLeavesNoConsumerStateOnTheBroker() {
        var destination = newDestination();
        try (var stream = reader.open(endpoint(), destination, StartPosition.LATEST)) {
            producer.send(endpoint(), destination, Message.of(Payload.text("hello")));
            pollUntil(stream, 1);
        }

        assertNoReaderStateLeftFor(destination);
    }

    @Test
    void rejectsAnUnsupportedDestinationOnProduce() {
        assertThatThrownBy(() -> producer.send(endpoint(), unsupportedDestination(), Message.of(Payload.text("x"))))
                .isInstanceOf(InvalidDestinationException.class);
    }

    @Test
    void rejectsAnUnsupportedDestinationOnRead() {
        assertThatThrownBy(() -> reader.open(endpoint(), unsupportedDestination(), StartPosition.LATEST))
                .isInstanceOf(InvalidDestinationException.class);
    }

    @Test
    void failsFastToProduceToAnUnreachableBroker() {
        var destination = newDestination();
        var start = Instant.now();

        assertThatThrownBy(() -> producer.send(unreachableEndpoint(), destination, Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);

        assertThat(Duration.between(start, Instant.now()))
                .as("a refused connection fails the send without waiting for the send timeout")
                .isLessThan(UNREACHABLE_TIMEOUT);
    }

    @Test
    void failsFastToReadFromAnUnreachableBroker() {
        var destination = newDestination();
        var start = Instant.now();

        assertThatThrownBy(() -> reader.open(unreachableEndpoint(), destination, StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);

        assertThat(Duration.between(start, Instant.now()))
                .as("a refused connection fails opening the reader without waiting for the open timeout")
                .isLessThan(UNREACHABLE_TIMEOUT);
    }

    @Test
    void passesTheConnectionTestOfAReachableBroker() {
        var outcome = tester.test(endpoint());

        assertThat(outcome.succeeded()).as(outcome.detail()).isTrue();
    }

    @Test
    void failsFastTheConnectionTestOfAnUnreachableBroker() {
        var start = Instant.now();

        var outcome = tester.test(unreachableEndpoint());

        assertThat(outcome.succeeded()).isFalse();
        assertThat(Duration.between(start, Instant.now())).isLessThan(UNREACHABLE_TIMEOUT);
    }

    @Test
    void failsToReadFromAMissingDestination() {
        assertThatThrownBy(() -> reader.open(endpoint(), missingDestination(), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);
    }

    /** Polls until exactly {@code count} messages arrived, failing if more or fewer do. */
    protected static List<ReceivedMessage> pollUntil(MessageStream stream, int count) {
        var received = new ArrayList<ReceivedMessage>();
        await().atMost(READ_TIMEOUT).untilAsserted(() -> {
            received.addAll(stream.poll(Duration.ofMillis(200)));
            assertThat(received).hasSizeGreaterThanOrEqualTo(count);
        });
        received.addAll(stream.poll(Duration.ofMillis(200)));
        assertThat(received).hasSize(count);
        return received;
    }

    protected static List<String> payloadsOf(List<ReceivedMessage> messages) {
        return messages.stream().map(m -> m.message().payload().content()).toList();
    }
}
