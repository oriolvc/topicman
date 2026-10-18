package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class KafkaMessageReaderIT {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private static Admin admin;
    private static KafkaProducer<String, byte[]> producer;

    private final KafkaMessageReader reader =
            new KafkaMessageReader(new KafkaProperties(Duration.ofSeconds(3), Duration.ofSeconds(3)));

    @BeforeAll
    static void connect() {
        admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()));
        producer = new KafkaProducer<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
                new StringSerializer(),
                new ByteArraySerializer());
    }

    @AfterAll
    static void disconnect() {
        producer.close();
        admin.close();
    }

    @Test
    void readsKeyHeadersPayloadTimestampAndPosition() {
        var topic = aTopic(1);
        var timestamp = Instant.parse("2026-10-16T10:00:00.123Z");
        var record = new ProducerRecord<>(topic, 0, timestamp.toEpochMilli(), "order-42", bytes("{\"id\":42}"));
        record.headers().add("source", bytes("topicman"));
        produce(record);

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            var received = pollUntil(stream, 1).getFirst();

            assertThat(received.message().key()).isEqualTo("order-42");
            assertThat(received.message().headers()).isEqualTo(Headers.of(Map.of("source", "topicman")));
            assertThat(received.message().payload()).isEqualTo(Payload.text("{\"id\":42}"));
            assertThat(received.timestamp()).isEqualTo(timestamp);
            assertThat(received.position()).isEqualTo(topic + "-0@0");
        }
    }

    @Test
    void readsEveryRetainedMessageFromEarliest() {
        var topic = aTopic(1);
        produce(topic, "first");
        produce(topic, "second");

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            assertThat(payloadsOf(pollUntil(stream, 2))).containsExactly("first", "second");
        }
    }

    @Test
    void readsOnlyMessagesProducedAfterOpeningFromLatest() {
        var topic = aTopic(1);
        produce(topic, "before");

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.LATEST)) {
            produce(topic, "after");

            assertThat(payloadsOf(pollUntil(stream, 1))).containsExactly("after");
        }
    }

    @Test
    void readsFromEveryPartition() {
        var topic = aTopic(3);
        for (int partition = 0; partition < 3; partition++) {
            produce(new ProducerRecord<>(topic, partition, null, bytes("p" + partition)));
        }

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            assertThat(payloadsOf(pollUntil(stream, 3))).containsExactlyInAnyOrder("p0", "p1", "p2");
        }
    }

    @Test
    void returnsNothingWhenNoMessageArrives() {
        var topic = aTopic(1);

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            assertThat(stream.poll(Duration.ofMillis(300))).isEmpty();
        }
    }

    @Test
    void toleratesRecordsWithEmptyKeysAndHeaderValuesOrBlankHeaderNames() {
        var topic = aTopic(1);
        var record = new ProducerRecord<>(topic, "", bytes("odd"));
        record.headers().add("no-value", null);
        record.headers().add(" ", bytes("blank name"));
        produce(record);

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            var received = pollUntil(stream, 1).getFirst();

            assertThat(received.message().key()).isNull();
            assertThat(received.message().headers()).isEqualTo(Headers.of(Map.of("no-value", "")));
        }
    }

    @Test
    void leavesNoConsumerGroupBehind() throws Exception {
        var topic = aTopic(1);
        produce(topic, "message");
        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            pollUntil(stream, 1);
        }

        var groups = admin.listGroups().all().get();

        assertThat(groups).isEmpty();
    }

    @Test
    void failsForAMissingTopicWithoutCreatingIt() throws Exception {
        var topic = "missing-" + UUID.randomUUID();

        assertThatThrownBy(() -> reader.open(endpoint(), Destination.topic(topic), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageContaining(topic);
        assertThat(admin.listTopics().names().get()).doesNotContain(topic);
    }

    @Test
    void failsWhenTheBrokerCannotBeReached() {
        var unreachable = new BrokerEndpoint(BrokerType.KAFKA, "localhost:1");

        assertThatThrownBy(() -> reader.open(unreachable, Destination.topic("orders"), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);
    }

    @Test
    void failsWhenTheAddressIsInvalid() {
        var invalid = new BrokerEndpoint(BrokerType.KAFKA, "not-an-address");

        assertThatThrownBy(() -> reader.open(invalid, Destination.topic("orders"), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);
    }

    @Test
    void rejectsAnExchangeDestination() {
        assertThatThrownBy(() -> reader.open(endpoint(), Destination.exchange("orders", ""), StartPosition.LATEST))
                .isInstanceOf(InvalidDestinationException.class);
    }

    private static BrokerEndpoint endpoint() {
        return new BrokerEndpoint(BrokerType.KAFKA, KAFKA.getBootstrapServers());
    }

    private static String aTopic(int partitions) {
        var topic = "topic-" + UUID.randomUUID();
        try {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1)))
                    .all()
                    .get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create topic " + topic, e);
        }
        return topic;
    }

    private static void produce(String topic, String payload) {
        produce(new ProducerRecord<>(topic, bytes(payload)));
    }

    private static void produce(ProducerRecord<String, byte[]> record) {
        try {
            producer.send(record).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not produce to " + record.topic(), e);
        }
    }

    private static List<ReceivedMessage> pollUntil(MessageStream stream, int count) {
        var received = new ArrayList<ReceivedMessage>();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            received.addAll(stream.poll(Duration.ofMillis(200)));
            assertThat(received).hasSizeGreaterThanOrEqualTo(count);
        });
        assertThat(received).hasSize(count);
        return received;
    }

    private static List<String> payloadsOf(List<ReceivedMessage> messages) {
        return messages.stream().map(m -> m.message().payload().content()).toList();
    }

    private static byte @Nullable [] bytes(@Nullable String value) {
        return value == null ? null : value.getBytes(UTF_8);
    }
}
